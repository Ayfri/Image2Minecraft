package io.github.ayfri.minecraft_art

import io.github.ayfri.minecraft_art.core.AppPaths
import io.github.ayfri.minecraft_art.core.BlockPalette
import io.github.ayfri.minecraft_art.core.BlockTag
import io.github.ayfri.minecraft_art.core.GenerationResult
import io.github.ayfri.minecraft_art.core.GenerationSettings
import io.github.ayfri.minecraft_art.core.Generator
import io.github.ayfri.minecraft_art.core.MinecraftVersion
import io.github.ayfri.minecraft_art.core.MinecraftVersions
import io.github.ayfri.minecraft_art.core.Platform
import io.github.ayfri.minecraft_art.core.Settings
import io.github.ayfri.minecraft_art.core.convertImage
import io.github.ayfri.minecraft_art.core.exportMaterials
import io.github.ayfri.minecraft_art.core.materials
import io.github.ayfri.minecraft_art.core.printPaletteReport
import io.github.ayfri.minecraft_art.ui.Align
import io.github.ayfri.minecraft_art.ui.Bitmap
import io.github.ayfri.minecraft_art.ui.Icon
import io.github.ayfri.minecraft_art.ui.KeyEvent
import io.github.ayfri.minecraft_art.ui.PointerAction
import io.github.ayfri.minecraft_art.ui.PointerEvent
import io.github.ayfri.minecraft_art.ui.Rect
import io.github.ayfri.minecraft_art.ui.Theme
import io.github.ayfri.minecraft_art.ui.UiRoot
import io.github.ayfri.minecraft_art.ui.backend.ProcessingRenderer
import io.github.ayfri.minecraft_art.ui.widgets.Button
import io.github.ayfri.minecraft_art.ui.widgets.ButtonStyle
import io.github.ayfri.minecraft_art.ui.widgets.Dropdown
import io.github.ayfri.minecraft_art.ui.widgets.ImageView
import io.github.ayfri.minecraft_art.ui.widgets.Slider
import io.github.ayfri.minecraft_art.ui.widgets.Toasts
import io.github.ayfri.minecraft_art.ui.widgets.Toggle
import io.github.ayfri.minecraft_art.ui.widgets.ToastKind
import io.github.ayfri.minecraft_art.ui.widgets.UsageEntry
import io.github.ayfri.minecraft_art.ui.widgets.UsageList
import io.github.ayfri.minecraft_art.ui.widgets.layoutRow
import processing.core.PApplet
import processing.event.KeyEvent as ProcessingKeyEvent
import processing.event.MouseEvent as ProcessingMouseEvent
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class Main : PApplet() {
	/** Preferences must be on disk before the widgets below read their initial value. */
	init {
		Settings.load()
	}

	private lateinit var renderer: ProcessingRenderer
	private val ui = UiRoot()
	private val palette = BlockPalette()
	private val generator = Generator(palette)
	private val worker = Executors.newSingleThreadExecutor { task -> Thread(task, "image2minecraft-worker").apply { isDaemon = true } }
	private val events = ConcurrentLinkedQueue<Any>()

	private var versions = emptyList<MinecraftVersion>()
	private var source: Bitmap? = null
	private var sourceName = "No image"
	private var sourceInfo = "Drop a file anywhere, or press Ctrl+O"
	private var sourceFile: File? = null

	@Volatile
	private var result: GenerationResult? = null

	@Volatile
	private var status = "Starting"

	@Volatile
	private var progress = 0f

	@Volatile
	private var busy = false

	private var lastFrame = 0L

	private val topBar = Rect()
	private val sourceCard = Rect()
	private val versionCard = Rect()
	private val settingsCard = Rect()
	private val exportCard = Rect()
	private val usageCard = Rect()
	private val statusBar = Rect()
	private val inputHeader = Rect()
	private val outputHeader = Rect()

	private val versionDropdown = Dropdown("Select a version") { index -> versions.getOrNull(index)?.let(::loadVersion) }
	private val reloadButton = Button("Re-extract textures", ButtonStyle.GHOST, Icon.REFRESH) { reextract() }
	private val openButton = Button("Open", ButtonStyle.SECONDARY, Icon.FOLDER) { openImage() }
	private val pasteButton = Button("Paste", ButtonStyle.SECONDARY, Icon.PASTE) { pasteImage() }
	private val clearButton = Button("Clear", ButtonStyle.SECONDARY, Icon.TRASH) { clearImage() }
	private val widthSlider = Slider("Width", 16f, 512f, Settings.blocksWide.toFloat(), 8f, { "${it.toInt()} blocks" }) {
		Settings.blocksWide = it.toInt()
	}
	private val ditherToggle = Toggle("Dithering", Settings.dithering) { Settings.dithering = it }
	private val tagToggles = BlockTag.entries.map { tag ->
		Toggle("Exclude ${tag.label.lowercase()}", tag in Settings.excluded) { applyFilters() }
	}
	private val generateButton = Button("Generate", ButtonStyle.PRIMARY, Icon.GENERATE) { generate() }
	private val saveButton = Button("Save PNG", ButtonStyle.SECONDARY, Icon.SAVE) { saveImage() }
	private val copyButton = Button("Copy", ButtonStyle.SECONDARY, Icon.COPY) { copyImage() }
	private val listButton = Button("Materials", ButtonStyle.SECONDARY, Icon.LIST) { exportList() }
	private val inputView = ImageView("Drop an image here")
	private val outputView = ImageView("Generate to see the result").apply {
		gridSize = BlockPalette.TEXTURE_SIZE
		showGrid = Settings.showGrid
	}
	private val fitButton = Button("Fit view", ButtonStyle.GHOST, Icon.ZOOM_FIT) { outputView.fit() }.apply { iconOnly = true }
	private val pixelButton = Button("Zoom to 100%", ButtonStyle.GHOST, Icon.IMAGE) { outputView.zoomToPixels() }.apply { iconOnly = true }
	private val gridButton = Button("Toggle block grid", ButtonStyle.GHOST, Icon.GRID) {
		outputView.showGrid = !outputView.showGrid
		Settings.showGrid = outputView.showGrid
	}.apply { iconOnly = true }
	private val usageList = UsageList("Generate an image to get its block list")
	private val toasts = Toasts()

	override fun settings() {
		size(1600, 940)
	}

	override fun setup() {
		AppPaths.ensure()
		surface.setTitle("Image2Minecraft")
		surface.setResizable(true)
		frameRate(60f)
		renderer = ProcessingRenderer(this)

		registerMethod("mouseEvent", this)
		registerMethod("keyEvent", this)
		Platform.onFileDropped(surface.native) { file -> submit { loadFile(file) } }

		ui.add(versionDropdown, reloadButton, openButton, pasteButton, clearButton, widthSlider, ditherToggle)
		tagToggles.forEach(ui::add)
		ui.add(generateButton, saveButton, copyButton, listButton, inputView, outputView)
		ui.add(fitButton, pixelButton, gridButton, usageList, toasts)
		layout()

		worker.submit(::discoverVersions)
		startupImage?.let { file -> submit { loadFile(file) } }
	}

	override fun draw() {
		val now = System.nanoTime()
		val delta = if (lastFrame == 0L) 1f / 60f else (now - lastFrame) / 1_000_000_000f
		lastFrame = now

		drainEvents()
		if (width.toFloat() != topBar.width || height - statusBar.bottom != 0f) layout()

		background(Theme.background)
		renderer.beginFrame(delta)
		drawChrome()
		ui.render(renderer)
	}

	private fun drainEvents() {
		while (true) {
			when (val event = events.poll()) {
				null -> return
				is PointerEvent -> ui.onPointer(event)
				is KeyEvent -> if (!ui.onKey(event)) shortcut(event)
				else -> Unit
			}
		}
	}

	private fun layout() {
		val padding = Theme.PADDING
		topBar.set(0f, 0f, width.toFloat(), 56f)
		statusBar.set(0f, height - 32f, width.toFloat(), 32f)

		val sidebarWidth = 320f
		val top = topBar.bottom + padding
		val bottom = statusBar.y - padding

		var y = top
		val x = padding
		val inner = sidebarWidth - 24f

		sourceCard.set(x, y, sidebarWidth, 130f)
		val sourceInner = sourceCard.inset(12f)
		layoutRow(Rect(sourceInner.x, sourceCard.bottom - 46f, sourceInner.width, 34f), 8f, listOf(openButton, pasteButton, clearButton))
		y = sourceCard.bottom + Theme.GAP

		versionCard.set(x, y, sidebarWidth, 112f)
		versionDropdown.place(x + 12f, y + 32f, inner, 34f)
		reloadButton.place(x + 12f, y + 74f, inner, 26f)
		y = versionCard.bottom + Theme.GAP

		val toggleHeight = 26f
		settingsCard.set(x, y, sidebarWidth, 122f + tagToggles.size * toggleHeight)
		widthSlider.place(x + 12f, y + 28f, inner, 42f)
		ditherToggle.place(x + 12f, y + 92f, inner, toggleHeight)
		tagToggles.forEachIndexed { index, toggle -> toggle.place(x + 12f, y + 120f + index * toggleHeight, inner, toggleHeight) }
		y = settingsCard.bottom + Theme.GAP

		exportCard.set(x, y, sidebarWidth, 116f)
		generateButton.place(x + 12f, y + 28f, inner, 38f)
		layoutRow(Rect(x + 12f, y + 72f, inner, 30f), 8f, listOf(saveButton, copyButton, listButton))
		y = exportCard.bottom + Theme.GAP

		usageCard.set(x, y, sidebarWidth, maxOf(90f, bottom - y))
		usageList.place(x + 12f, y + 46f, inner, usageCard.height - 58f)

		val viewX = sidebarWidth + padding * 2f
		val viewWidth = (width - viewX - padding - Theme.GAP) / 2f
		inputHeader.set(viewX, top, viewWidth, 24f)
		outputHeader.set(viewX + viewWidth + Theme.GAP, top, viewWidth, 24f)
		inputView.place(inputHeader.x, top + 28f, viewWidth, bottom - top - 28f)
		outputView.place(outputHeader.x, top + 28f, viewWidth, bottom - top - 28f)

		val buttons = listOf(gridButton, pixelButton, fitButton)
		buttons.forEachIndexed { index, button -> button.place(outputHeader.right - 26f - index * 30f, top - 2f, 26f, 26f) }
		toasts.place(width - 400f - padding, height - 120f, 400f, 100f)
	}

	private fun drawChrome() {
		renderer.fill(topBar, Theme.surface)
		renderer.line(0f, topBar.bottom, width.toFloat(), topBar.bottom, Theme.border)
		renderer.text("Image2Minecraft", Theme.PADDING, topBar.centerY - 1f, Theme.title)
		val titleWidth = renderer.textWidth("Image2Minecraft", Theme.title)
		renderer.text("pixel art from your own Minecraft install", Theme.PADDING + titleWidth + 12f, topBar.centerY, Theme.small)

		val paletteInfo = if (palette.total == 0) "No palette loaded" else "${palette.size} of ${palette.total} block faces"
		renderer.text(paletteInfo, width - Theme.PADDING, topBar.centerY, Theme.small.copy(align = Align.RIGHT))

		card(sourceCard, "SOURCE")
		val sourceInner = sourceCard.inset(12f)
		renderer.textEllipsis(sourceName, sourceInner.x, sourceCard.y + 44f, sourceInner.width, Theme.body)
		renderer.textEllipsis(sourceInfo, sourceInner.x, sourceCard.y + 64f, sourceInner.width, Theme.small)

		card(versionCard, "MINECRAFT VERSION")
		card(settingsCard, "SETTINGS")
		card(exportCard, "EXPORT")
		card(usageCard, "BLOCKS USED")

		val distinct = result?.usage?.size ?: 0
		if (distinct > 0) {
			renderer.text("$distinct types", usageCard.right - 12f, usageCard.y + 22f, Theme.small.copy(align = Align.RIGHT))
		}

		renderer.text("INPUT", inputHeader.x, inputHeader.centerY, Theme.heading)
		renderer.text(inputSummary(), inputHeader.right, inputHeader.centerY, Theme.small.copy(align = Align.RIGHT))
		renderer.text("OUTPUT", outputHeader.x, outputHeader.centerY, Theme.heading)
		renderer.text(outputSummary(), outputHeader.right - 96f, outputHeader.centerY, Theme.small.copy(align = Align.RIGHT))

		renderer.fill(statusBar, Theme.surface)
		renderer.line(0f, statusBar.y, width.toFloat(), statusBar.y, Theme.border)
		renderer.text(status, Theme.PADDING, statusBar.centerY, Theme.small.copy(color = if (busy) Theme.accent else Theme.textMuted))
		if (busy) {
			val bar = Rect(200f, statusBar.centerY - 3f, 220f, 6f)
			renderer.fill(bar, Theme.surfaceHover, 3f)
			renderer.fill(Rect(bar.x, bar.y, bar.width * progress.coerceIn(0f, 1f), bar.height), Theme.accent, 3f)
		}
		renderer.text(shortcutsHint(), width - Theme.PADDING, statusBar.centerY, Theme.small.copy(align = Align.RIGHT, color = Theme.textFaint))
	}

	private fun card(rect: Rect, title: String) {
		renderer.fill(rect, Theme.surface, Theme.RADIUS)
		renderer.stroke(rect, Theme.border, Theme.RADIUS)
		renderer.text(title, rect.x + 12f, rect.y + 20f, Theme.heading)
	}

	private fun inputSummary(): String {
		val image = source ?: return "empty"
		return "${image.width} x ${image.height} px"
	}

	private fun outputSummary(): String {
		val current = result ?: return "empty"
		return "${current.blocksWide} x ${current.blocksHigh} blocks - ${current.image.width} x ${current.image.height} px"
	}

	private fun shortcutsHint(): String {
		val estimate = source?.let { image ->
			val blocks = widthSlider.value.toInt()
			val high = (blocks / image.aspectRatio).roundToInt().coerceAtLeast(1)
			"$blocks x $high blocks - ${blocks * BlockPalette.TEXTURE_SIZE} x ${high * BlockPalette.TEXTURE_SIZE} px"
		}
		return estimate ?: "Ctrl+O open - Ctrl+G generate - Ctrl+S save - Ctrl+C copy"
	}

	private fun discoverVersions() {
		versions = MinecraftVersions.installed()
		versionDropdown.items = versions.map(MinecraftVersion::id)
		if (versions.isEmpty()) {
			status = "No Minecraft install found in ${MinecraftVersions.gameDirectory}"
			toasts.show("No Minecraft version found", ToastKind.ERROR)
			return
		}

		val remembered = versions.indexOfFirst { it.id == Settings.version }
		val index = if (remembered >= 0) remembered else 0
		versionDropdown.selected = index
		loadVersion(versions[index])
	}

	private fun loadVersion(version: MinecraftVersion) {
		Settings.version = version.id
		submit {
			status = "Extracting textures from ${version.id}"
			MinecraftVersions.extractTextures(version) { progress = it * 0.7f }
			status = "Loading palette"
			progress = 0.8f
			palette.load(AppPaths.textures(version.id).toFile())
			applyFilters()
			status = "Palette ready - ${palette.size} block faces from ${version.id}"
			toasts.show("${palette.size} blocks loaded from ${version.id}", ToastKind.SUCCESS)
		}
	}

	private fun reextract() {
		val version = versions.getOrNull(versionDropdown.selected) ?: return
		submit {
			status = "Clearing texture cache"
			AppPaths.textures(version.id).toFile().listFiles()?.forEach(File::delete)
		}
		loadVersion(version)
	}

	private fun applyFilters() {
		val excluded = BlockTag.entries.filterIndexedTo(mutableSetOf()) { index, _ -> tagToggles[index].checked }
		Settings.excluded = excluded
		palette.excluded = excluded
	}

	private fun openImage() = submit {
		val file = Platform.openImage(Platform.frameOf(surface.native), Settings.imageDirectory) ?: return@submit
		loadFile(file)
	}

	private fun loadFile(file: File) {
		if (!Platform.isImage(file)) {
			toasts.show("${file.extension.uppercase()} files are not supported", ToastKind.ERROR)
			return
		}

		val bitmap = Platform.readImage(file)
		if (bitmap == null) {
			toasts.show("Could not read ${file.name}", ToastKind.ERROR)
			return
		}

		Settings.imageDirectory = file.parentFile
		sourceFile = file
		setSource(bitmap, file.name, "${file.length() / 1024} KB")
	}

	private fun pasteImage() = submit {
		val bitmap = Platform.clipboardImage()
		if (bitmap == null) {
			toasts.show("No image in the clipboard", ToastKind.WARNING)
			return@submit
		}
		sourceFile = null
		setSource(bitmap, "Clipboard image", "pasted")
	}

	private fun setSource(bitmap: Bitmap, name: String, details: String) {
		source = bitmap
		sourceName = name
		sourceInfo = "${bitmap.width} x ${bitmap.height} px - $details"
		inputView.bitmap = bitmap
		status = "Loaded $name"
	}

	private fun clearImage() {
		source = null
		sourceFile = null
		result = null
		sourceName = "No image"
		sourceInfo = "Drop a file anywhere, or press Ctrl+O"
		inputView.bitmap = null
		outputView.bitmap = null
		usageList.entries = emptyList()
		status = "Ready"
	}

	private fun generate() {
		val image = source ?: return toasts.show("Load an image first", ToastKind.WARNING)
		if (palette.size == 0) return toasts.show("No block palette loaded", ToastKind.WARNING)

		submit {
			val settings = GenerationSettings(widthSlider.value.toInt(), ditherToggle.checked)
			status = "Generating ${settings.blocksWide} blocks wide"
			val generated = generator.generate(image, settings) { progress = it }
			if (generated == null) {
				toasts.show("Generation failed, the palette is empty", ToastKind.ERROR)
				return@submit
			}

			result = generated
			outputView.bitmap = generated.preview
			usageList.entries = usageEntries(generated)
			status = "Done in ${generated.elapsed} - ${generated.blockCount} blocks, ${generated.usage.size} types"
			toasts.show("Generated in ${generated.elapsed}", ToastKind.SUCCESS)
		}
	}

	private fun usageEntries(generated: GenerationResult): List<UsageEntry> {
		val colors = palette.entries.associate { it.name to it.color }
		val list = materials(generated.usage)
		val top = (list.firstOrNull()?.count ?: 1).toFloat()
		return list.map { material ->
			val color = colors[material.name] ?: Theme.surfaceHover
			UsageEntry(material.name.replace('_', ' '), color, material.count, material.count / top)
		}
	}

	private fun saveImage() {
		val generated = result ?: return toasts.show("Nothing to save yet", ToastKind.WARNING)
		submit {
			val suggested = (sourceFile?.nameWithoutExtension ?: "image") + "-minecraft.png"
			val file = Platform.saveFile(Platform.frameOf(surface.native), Settings.exportDirectory, suggested) ?: return@submit
			status = "Saving ${file.name}"
			Platform.savePng(file, generated.image)
			Settings.exportDirectory = file.parentFile
			toasts.show("Saved ${file.name}", ToastKind.SUCCESS)
			status = "Saved to ${file.absolutePath}"
		}
	}

	private fun copyImage() {
		val generated = result ?: return toasts.show("Nothing to copy yet", ToastKind.WARNING)
		submit {
			Platform.copyToClipboard(generated.image)
			toasts.show("Image copied to the clipboard", ToastKind.SUCCESS)
		}
	}

	private fun exportList() {
		val generated = result ?: return toasts.show("Generate an image first", ToastKind.WARNING)
		submit {
			val suggested = (sourceFile?.nameWithoutExtension ?: "image") + "-materials.txt"
			val file = Platform.saveFile(Platform.frameOf(surface.native), Settings.exportDirectory, suggested) ?: return@submit
			val settings = GenerationSettings(generated.blocksWide, ditherToggle.checked)
			exportMaterials(file, generated, settings, versionDropdown.value ?: "unknown")
			Settings.exportDirectory = file.parentFile
			toasts.show("Material list written to ${file.name}", ToastKind.SUCCESS)
		}
	}

	private fun submit(action: () -> Unit) {
		worker.submit {
			busy = true
			progress = 0f
			runCatching(action).onFailure {
				it.printStackTrace()
				status = "Error: ${it.message}"
				toasts.show(it.message ?: "Unexpected error", ToastKind.ERROR)
			}
			busy = false
			progress = 0f
		}
	}

	private fun shortcut(event: KeyEvent) {
		when {
			event.shortcut('o') -> openImage()
			event.shortcut('g') -> generate()
			event.shortcut('s') -> saveImage()
			event.shortcut('c') -> copyImage()
			event.shortcut('v') -> pasteImage()
			event.shortcut('e') -> exportList()
			event.char == 'f' -> outputView.fit()
			event.char == '1' -> outputView.zoomToPixels()
			event.char == 'g' -> {
				outputView.showGrid = !outputView.showGrid
				Settings.showGrid = outputView.showGrid
			}
		}
	}

	/** Called by reflection through `registerMethod("mouseEvent", this)`, runs on the AWT thread. */
	fun mouseEvent(event: ProcessingMouseEvent) {
		val action = when (event.action) {
			ProcessingMouseEvent.PRESS -> PointerAction.PRESS
			ProcessingMouseEvent.RELEASE -> PointerAction.RELEASE
			ProcessingMouseEvent.DRAG -> PointerAction.DRAG
			ProcessingMouseEvent.MOVE -> PointerAction.MOVE
			ProcessingMouseEvent.WHEEL -> PointerAction.WHEEL
			ProcessingMouseEvent.EXIT -> PointerAction.EXIT
			else -> return
		}
		val scroll = if (action == PointerAction.WHEEL) event.count.toFloat() else 0f
		events += PointerEvent(action, event.x.toFloat(), event.y.toFloat(), event.button, scroll, maxOf(1, event.count))
	}

	/** Called by reflection through `registerMethod("keyEvent", this)`, runs on the AWT thread. */
	fun keyEvent(event: ProcessingKeyEvent) {
		if (event.action != ProcessingKeyEvent.PRESS) return
		events += KeyEvent(event.key, event.keyCode, event.isControlDown, event.isShiftDown)
	}

	override fun exit() {
		worker.shutdownNow()
		super.exit()
	}
}

/** Image passed on the command line, so the app can be associated with image files or started from a shortcut. */
private var startupImage: File? = null

fun main(args: Array<String>) = when (args.firstOrNull()) {
	"--palette" -> printPaletteReport(args.getOrNull(1))
	"--convert" -> convertImage(args.drop(1))
	else -> {
		startupImage = args.firstOrNull()?.let(::File)?.takeIf(Platform::isImage)
		PApplet.main(Main::class.java)
	}
}
