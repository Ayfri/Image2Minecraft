package io.github.ayfri.minecraft_art.core

import io.github.ayfri.minecraft_art.ui.Bitmap
import java.awt.Component
import java.awt.FileDialog
import java.awt.Frame
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDropEvent
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import javax.swing.SwingUtilities

/** Everything that talks to the desktop: native dialogs, clipboard and drag and drop. */
data object Platform {
	/** Formats `ImageIO` can read out of the box, WebP has no bundled reader and is deliberately absent. */
	val IMAGE_EXTENSIONS = setOf("png", "jpg", "jpeg", "bmp", "gif")

	fun frameOf(native: Any?): Frame? {
		val component = native as? Component ?: return null
		return SwingUtilities.getWindowAncestor(component) as? Frame
	}

	private val IMAGE_PATTERN = IMAGE_EXTENSIONS.joinToString(";") { "*.$it" }
	private val IMAGE_FILTERS = listOf(FileFilter("Images ($IMAGE_PATTERN)", IMAGE_PATTERN), FileFilter("All files", "*.*"))

	/** Modern Explorer picker where it exists, [FileDialog] elsewhere, which is still the native dialog on macOS and Linux. */
	fun openImage(frame: Frame?, directory: File?): File? {
		if (WindowsFileDialog.available) return WindowsFileDialog.open("Open an image", directory, IMAGE_FILTERS)
		return dialog(frame, "Open an image", FileDialog.LOAD) {
			it.directory = directory?.absolutePath
			/** Windows reads this as a filter pattern, other platforms simply ignore it. */
			it.file = IMAGE_PATTERN
			it.setFilenameFilter { _, name -> name.substringAfterLast('.', "").lowercase() in IMAGE_EXTENSIONS }
		}
	}

	fun saveFile(frame: Frame?, directory: File?, defaultName: String): File? {
		val extension = defaultName.substringAfterLast('.', "")
		if (WindowsFileDialog.available) {
			val filters = listOf(FileFilter("${extension.uppercase()} file (*.$extension)", "*.$extension"), FileFilter("All files", "*.*"))
			return WindowsFileDialog.save("Save as", directory, defaultName, filters)
		}
		return dialog(frame, "Save as", FileDialog.SAVE) {
			it.directory = directory?.absolutePath
			it.file = defaultName
		}
	}

	private fun dialog(frame: Frame?, title: String, mode: Int, configure: (FileDialog) -> Unit): File? {
		val dialog = FileDialog(frame, title, mode)
		configure(dialog)
		dialog.isVisible = true
		val name = dialog.file ?: return null
		return File(dialog.directory ?: ".", name)
	}

	fun readImage(file: File) = runCatching { ImageIO.read(file) }.getOrNull()?.let(Bitmap::of)

	fun clipboardImage(): Bitmap? {
		val contents = runCatching { Toolkit.getDefaultToolkit().systemClipboard.getContents(null) }.getOrNull() ?: return null
		if (contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
			val image = runCatching { contents.getTransferData(DataFlavor.imageFlavor) as? Image }.getOrNull()
			if (image != null) return Bitmap.of(toBuffered(image))
		}
		if (!contents.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return null

		val files = runCatching { contents.getTransferData(DataFlavor.javaFileListFlavor) as? List<*> }.getOrNull()
		return files?.filterIsInstance<File>()?.firstOrNull(::isImage)?.let(::readImage)
	}

	fun copyToClipboard(bitmap: Bitmap) {
		val image = bitmap.toBufferedImage()
		val transferable = object : Transferable {
			override fun getTransferDataFlavors() = arrayOf(DataFlavor.imageFlavor)
			override fun isDataFlavorSupported(flavor: DataFlavor?) = flavor == DataFlavor.imageFlavor
			override fun getTransferData(flavor: DataFlavor?): Any = image
		}
		Toolkit.getDefaultToolkit().systemClipboard.setContents(transferable, null)
	}

	fun isImage(file: File) = file.isFile && file.extension.lowercase() in IMAGE_EXTENSIONS

	fun onFileDropped(native: Any?, onDrop: (File) -> Unit) {
		val component = native as? Component ?: return
		component.dropTarget = DropTarget(component, DnDConstants.ACTION_COPY, object : DropTargetAdapter() {
			override fun drop(event: DropTargetDropEvent) {
				event.acceptDrop(DnDConstants.ACTION_COPY)
				val files = runCatching { event.transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<*> }.getOrNull()
				files?.filterIsInstance<File>()?.firstOrNull()?.let(onDrop)
				event.dropComplete(true)
			}
		})
	}

	fun savePng(file: File, bitmap: Bitmap) {
		val target = if (file.extension.isEmpty()) File("${file.path}.png") else file
		ImageIO.write(bitmap.toBufferedImage(), "png", target)
	}

	private fun toBuffered(image: Image) = image as? BufferedImage ?: BufferedImage(
		image.getWidth(null),
		image.getHeight(null),
		BufferedImage.TYPE_INT_ARGB,
	).also {
		val graphics = it.createGraphics()
		graphics.drawImage(image, 0, 0, null)
		graphics.dispose()
	}
}
