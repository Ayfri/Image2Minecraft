package io.github.ayfri.minecraft_art.core

import java.io.File
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.SegmentAllocator
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_BYTE
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_SHORT
import java.nio.charset.StandardCharsets

/** One entry of the file type combo, [spec] is a semicolon separated pattern list such as `*.png;*.jpg`. */
data class FileFilter(val label: String, val spec: String)

/**
 * Windows Common Item Dialog (`IFileOpenDialog` and `IFileSaveDialog`) driven through the FFM API. It is the picker
 * Explorer itself uses, with the places sidebar, search box, breadcrumb bar and a real file type combo, where AWT's
 * `FileDialog` still opens the Windows 2000 era dialog and ignores filters outside of Windows.
 *
 * Every call returns null when the platform, the libraries or the COM calls are unavailable, so the caller can fall
 * back to `FileDialog` without inspecting anything.
 */
data object WindowsFileDialog {
	private val linker = Linker.nativeLinker()

	/** Library handles and the GUID constants outlive every dialog, so they live in one arena kept for the process. */
	private val arena = Arena.ofAuto()

	private val bindings = runCatching {
		if (!System.getProperty("os.name").orEmpty().startsWith("Windows")) return@runCatching null
		Bindings(
			SymbolLookup.libraryLookup("ole32.dll", arena),
			SymbolLookup.libraryLookup("user32.dll", arena),
			SymbolLookup.libraryLookup("shell32.dll", arena),
		)
	}.getOrNull()

	val available get() = bindings != null

	fun open(title: String, directory: File?, filters: List<FileFilter>) =
		show(CLSID_FILE_OPEN, IID_FILE_OPEN, title, directory, null, FOS_FILE_MUST_EXIST or FOS_PATH_MUST_EXIST or FOS_FORCE_FILESYSTEM, filters)

	fun save(title: String, directory: File?, fileName: String, filters: List<FileFilter>) =
		show(CLSID_FILE_SAVE, IID_FILE_SAVE, title, directory, fileName, FOS_OVERWRITE_PROMPT or FOS_PATH_MUST_EXIST or FOS_FORCE_FILESYSTEM, filters)

	private fun show(clsid: String, iid: String, title: String, directory: File?, fileName: String?, options: Int, filters: List<FileFilter>): File? {
		val native = bindings ?: return null
		return runCatching {
			/** The Common Item Dialog requires a single threaded apartment, and OLE1 DDE support only slows initialisation down. */
			val initialized = native.coInitializeEx(MemorySegment.NULL, COINIT_APARTMENT_THREADED or COINIT_DISABLE_OLE1DDE)
			if (initialized < 0) return null

			try {
				Arena.ofConfined().use { scope -> dialog(native, scope, clsid, iid, title, directory, fileName, options, filters) }
			} finally {
				native.coUninitialize()
			}
		}.getOrNull()
	}

	private fun dialog(
		native: Bindings,
		scope: Arena,
		clsid: String,
		iid: String,
		title: String,
		directory: File?,
		fileName: String?,
		options: Int,
		filters: List<FileFilter>,
	): File? {
		val out = scope.allocate(ADDRESS)
		if (native.coCreateInstance(scope.guid(clsid), MemorySegment.NULL, CLSCTX_INPROC_SERVER, scope.guid(iid), out) < 0) return null

		val dialog = out.get(ADDRESS, 0).reinterpret(POINTER_SIZE)
		try {
			native.call(dialog, SET_OPTIONS, options)
			native.call(dialog, SET_TITLE, scope.wide(title))
			fileName?.let { native.call(dialog, SET_FILE_NAME, scope.wide(it)) }
			if (filters.isNotEmpty()) native.call(dialog, SET_FILE_TYPES, filters.size, scope.filterSpecs(filters))
			directory?.takeIf(File::isDirectory)?.let { folder ->
				native.shellItem(scope, folder)?.let { item ->
					native.call(dialog, SET_FOLDER, item)
					native.call(item, RELEASE)
				}
			}

			/** No portable way exists to reach the sketch HWND, and the foreground window is ours whenever a click opened the dialog. */
			if (native.call(dialog, SHOW, native.foregroundWindow()) < 0) return null

			val result = scope.allocate(ADDRESS)
			if (native.call(dialog, GET_RESULT, result) < 0) return null
			val item = result.get(ADDRESS, 0).reinterpret(POINTER_SIZE)
			return try {
				native.path(scope, item)
			} finally {
				native.call(item, RELEASE)
			}
		} finally {
			native.call(dialog, RELEASE)
		}
	}

	/** Downcall handles and the vtable dispatch used to talk to COM, grouped so a missing library disables the whole picker. */
	private class Bindings(ole32: SymbolLookup, user32: SymbolLookup, shell32: SymbolLookup) {
		private val coInitialize = downcall(ole32, "CoInitializeEx", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT))
		private val coUninitializeHandle = downcall(ole32, "CoUninitialize", FunctionDescriptor.ofVoid())
		private val coCreate = downcall(ole32, "CoCreateInstance", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, ADDRESS, ADDRESS))
		private val coTaskMemFree = downcall(ole32, "CoTaskMemFree", FunctionDescriptor.ofVoid(ADDRESS))
		private val getForegroundWindow = downcall(user32, "GetForegroundWindow", FunctionDescriptor.of(ADDRESS))
		private val createShellItem = downcall(shell32, "SHCreateItemFromParsingName", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS))

		fun coInitializeEx(reserved: MemorySegment, flags: Int) = coInitialize.invoke(reserved, flags) as Int
		fun coUninitialize() = coUninitializeHandle.invoke()
		fun coCreateInstance(clsid: MemorySegment, outer: MemorySegment, context: Int, iid: MemorySegment, out: MemorySegment) =
			coCreate.invoke(clsid, outer, context, iid, out) as Int

		fun foregroundWindow() = getForegroundWindow.invoke() as MemorySegment

		fun shellItem(scope: Arena, folder: File): MemorySegment? {
			val out = scope.allocate(ADDRESS)
			val code = createShellItem.invoke(scope.wide(folder.absolutePath), MemorySegment.NULL, scope.guid(IID_SHELL_ITEM), out) as Int
			return if (code < 0) null else out.get(ADDRESS, 0).reinterpret(POINTER_SIZE)
		}

		fun path(scope: Arena, item: MemorySegment): File? {
			val out = scope.allocate(ADDRESS)
			if (call(item, GET_DISPLAY_NAME, SIGDN_FILESYS_PATH, out) < 0) return null
			val text = out.get(ADDRESS, 0)
			if (text == MemorySegment.NULL) return null
			return try {
				File(text.reinterpret(MAX_PATH_BYTES).getString(0, StandardCharsets.UTF_16LE))
			} finally {
				coTaskMemFree.invoke(text)
			}
		}

		/** Calls slot [slot] of the object's vtable, an `Int` argument maps to a `DWORD` and everything else to a pointer. */
		fun call(target: MemorySegment, slot: Int, vararg arguments: Any): Int {
			val layouts = arguments.map { if (it is Int) JAVA_INT else ADDRESS }
			val vtable = target.get(ADDRESS, 0).reinterpret((slot + 1) * POINTER_SIZE)
			val function = vtable.get(ADDRESS, slot * POINTER_SIZE)
			val handle = linker.downcallHandle(function, FunctionDescriptor.of(JAVA_INT, ADDRESS, *layouts.toTypedArray<MemoryLayout>()))
			return handle.invokeWithArguments(listOf(target) + arguments) as Int
		}

		private fun downcall(lookup: SymbolLookup, name: String, descriptor: FunctionDescriptor) =
			linker.downcallHandle(lookup.findOrThrow(name), descriptor)
	}

	private fun SegmentAllocator.wide(value: String) = allocateFrom(value, StandardCharsets.UTF_16LE)

	/** Array of `COMDLG_FILTERSPEC`, two wide string pointers per entry. */
	private fun Arena.filterSpecs(filters: List<FileFilter>): MemorySegment {
		val specs = allocate(POINTER_SIZE * 2 * filters.size, POINTER_SIZE)
		filters.forEachIndexed { index, filter ->
			specs.set(ADDRESS, index * 2 * POINTER_SIZE, wide(filter.label))
			specs.set(ADDRESS, (index * 2 + 1) * POINTER_SIZE, wide(filter.spec))
		}
		return specs
	}

	/** Parses `dc1c5a9c-e88a-4dde-a5a1-60f82a20aef7` into the mixed-endian 16 byte `GUID` struct COM expects. */
	private fun Arena.guid(value: String): MemorySegment {
		val hex = value.replace("-", "")
		val guid = allocate(16, 8)
		guid.set(JAVA_INT, 0, hex.substring(0, 8).toUInt(16).toInt())
		guid.set(JAVA_SHORT, 4, hex.substring(8, 12).toInt(16).toShort())
		guid.set(JAVA_SHORT, 6, hex.substring(12, 16).toInt(16).toShort())
		for (byte in 0..<8) guid.set(JAVA_BYTE, 8L + byte, hex.substring(16 + byte * 2, 18 + byte * 2).toInt(16).toByte())
		return guid
	}

	private const val POINTER_SIZE = 8L
	private const val MAX_PATH_BYTES = 66_000L

	private const val CLSID_FILE_OPEN = "dc1c5a9c-e88a-4dde-a5a1-60f82a20aef7"
	private const val IID_FILE_OPEN = "d57c7288-d4ad-4768-be02-9d969532d960"
	private const val CLSID_FILE_SAVE = "c0b4e2f3-ba21-4773-8dba-335ec946eb8b"
	private const val IID_FILE_SAVE = "84bccd23-5fde-4cdb-aea4-af64b83d78ab"
	/** `IShellItem2`, because `SHCreateItemFromParsingName` answers `E_NOINTERFACE` for plain `IShellItem` on Windows 11. */
	private const val IID_SHELL_ITEM = "7e9fb0d3-919f-4307-ab2e-9b1860310c93"

	/** `IFileDialog` vtable slots, `IFileOpenDialog` and `IFileSaveDialog` both start with this exact layout. */
	private const val RELEASE = 2
	private const val SHOW = 3
	private const val SET_FILE_TYPES = 4
	private const val SET_OPTIONS = 9
	private const val SET_FOLDER = 12
	private const val SET_FILE_NAME = 15
	private const val SET_TITLE = 17
	private const val GET_RESULT = 20

	/** `IShellItem::GetDisplayName`, the only slot of that interface this code needs. */
	private const val GET_DISPLAY_NAME = 5
	private val SIGDN_FILESYS_PATH = 0x8005_8000.toInt()

	private const val COINIT_APARTMENT_THREADED = 0x2
	private const val COINIT_DISABLE_OLE1DDE = 0x4
	private const val CLSCTX_INPROC_SERVER = 0x1

	private const val FOS_OVERWRITE_PROMPT = 0x2
	private const val FOS_FORCE_FILESYSTEM = 0x40
	private const val FOS_PATH_MUST_EXIST = 0x800
	private const val FOS_FILE_MUST_EXIST = 0x1000
}

