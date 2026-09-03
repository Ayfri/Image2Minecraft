package io.github.ayfri.minecraft_art

import io.github.ayfri.minecraft_art.gui.Gui
import processing.core.PApplet
import processing.core.PGraphics
import processing.core.PImage
import processing.event.MouseEvent
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class Main : PApplet() {
	lateinit var textureManager: TextureManager
		private set
	lateinit var gui: Gui
		private set
	lateinit var output: PGraphics
	/** Stays null until an image is picked, the sketch has to keep drawing its UI in the meantime. */
	var inputImage: PImage? = null
	var input: File? = null
		private set

	val threadPool = ThreadPoolExecutor(CORES, CORES * 2, 5, TimeUnit.MINUTES, LinkedBlockingQueue())

	override fun settings() {
		println("Starting")
		size(1400, 700)
		registerMethod("mouseEvent", this)
		registerMethod("pre", this)
		textureManager = TextureManager(this)
	}

	override fun setup() {
		input = localFileOrNull("files/outputColor.png")
		output = createGraphics(16, 16)
		gui = Gui(this)
		gui.sliders[0].max = 20f

		runCatching { threadPool.submit(::loadBlocks).get() }.onFailure(Throwable::printStackTrace)

		inputImage = input?.let { loadImage(it.path)?.apply(PImage::loadPixels) }

		surface.setTitle("Image2Minecraft by Ayfri")
		runCatching(::resetBlocks).onFailure(Throwable::printStackTrace)
		println("Init ended")
	}

	override fun draw() {
		background(180)
		gui.draw()
	}

	fun loadBlocks() = runCatching {
		textureManager.init()
		println("Blocks loaded : ${Blocks.size}")
	}.onFailure(Throwable::printStackTrace)

	/** Called by reflection by [PApplet.selectInput], the name is part of the `SelectImageButton` callback contract. */
	fun chooseFile(file: File?) {
		if (file == null) return
		inputImage = loadImage(file.absolutePath)
	}

	/** Called by reflection through `registerMethod("pre", this)`, runs once per frame before [draw]. */
	fun pre() {
		val ratio = gui.sliders[0].value
		val image = inputImage
		gui.texts["size"]?.text = "Size : ${(input?.length() ?: 0) / 1024} KB"
		gui.texts["width"]?.text = "Width : ${image?.width ?: 0} px"
		gui.texts["height"]?.text = "Height : ${image?.height ?: 0} px"
		gui.texts["pixels"]?.text = "Number of\npixels : ${(image?.width ?: 0) * (image?.height ?: 0)}"
		gui.texts["out-width"]?.text = "Width : ${((image?.width ?: 0) * ratio).toInt()} blocks"
		gui.texts["out-height"]?.text = "Height : ${((image?.height ?: 0) * ratio).toInt()} blocks"
	}

	/** Called by reflection through `registerMethod("mouseEvent", this)`. */
	fun mouseEvent(event: MouseEvent) {
		threadPool.submit { gui.mouseEvent(event) }
	}

	/** Called by reflection by [PApplet.selectOutput], the name is part of the `SaveButton` callback contract. */
	fun save(file: File?) {
		output.save(file?.absolutePath ?: return)
		println("Image saved !")
	}

	companion object {
		private val CORES = Runtime.getRuntime().availableProcessors()
	}
}

fun main() = PApplet.main(Main::class.java)
