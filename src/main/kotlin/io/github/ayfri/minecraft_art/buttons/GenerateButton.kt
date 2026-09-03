package io.github.ayfri.minecraft_art.buttons

import io.github.ayfri.minecraft_art.Blocks
import io.github.ayfri.minecraft_art.Main
import io.github.ayfri.minecraft_art.getNearestResizedBlock
import io.github.ayfri.minecraft_art.registerBlocksFromVersion
import processing.core.PApplet
import processing.core.PGraphics
import kotlin.time.measureTime

class GenerateButton(sketch: Main) : Button(sketch, sketch.width / 2 - 200, sketch.height - 100, 150, 80) {
	init {
		text = "Generate"
		println("Loading blocks")
	}

	override fun onClick() {
		sketch.threadPool.submit(::generate)
		registerBlocksFromVersion("21w15a")
	}

	fun generate() {
		val inputImage = sketch.inputImage ?: return println("No image selected.")
		if (Blocks.textures.isEmpty()) return println("No block texture loaded, put block PNGs in the 'blocks' directory.")

		println("${ESCAPE}[31mStarting Generate process. ")
		val ratio = sketch.gui.sliders[0].value
		val loading = sketch.gui.loadingBar
		val resizedImage = inputImage.copy()
		var x = -BLOCK_SIZE
		var y = 0

		Blocks.resetUses()
		resizedImage.resize((inputImage.width * ratio).toInt(), (inputImage.height * ratio).toInt())
		resizedImage.loadPixels()
		sketch.output = sketch.createGraphics(resizedImage.width * BLOCK_SIZE, resizedImage.height * BLOCK_SIZE)
		println("Number of pixels : ${resizedImage.pixels.size}")

		val elapsed = measureTime {
			sketch.output.beginDraw()
			for (i in resizedImage.pixels.indices) {
				x += BLOCK_SIZE
				if (x >= resizedImage.width * BLOCK_SIZE) {
					x = 0
					y += BLOCK_SIZE
				}

				val block = getNearestResizedBlock(sketch.gui.outputZone, resizedImage.pixels[i]) ?: continue
				sketch.output.image(block, x.toFloat(), y.toFloat(), BLOCK_SIZE.toFloat(), BLOCK_SIZE.toFloat())

				loading.percentage = PApplet.map(i.toFloat(), 0f, resizedImage.pixels.size.toFloat(), 0f, 100f).toInt()
				loading.draw()
			}
			sketch.output.endDraw()
		}

		loading.beginDraw()
		loading.clear()
		loading.endDraw()

		println("$elapsed time took.")
		println("Image proceeded")
		Blocks.used.toSortedMap().forEach { (name, count) -> println("${name.removeSuffix(".png")} : $count") }
	}

	/**
	 * This is for testing other way of rendering.
	 */
	fun justPutInputInOutput() {
		val inputImage = sketch.inputImage ?: return
		val graphics: PGraphics = sketch.createGraphics(inputImage.width * BLOCK_SIZE, inputImage.height * BLOCK_SIZE)
		graphics.beginDraw()
		graphics.image(sketch.gui.inputZone, 0f, 0f)
		graphics.endDraw()
		sketch.output = graphics
	}

	companion object {
		private const val BLOCK_SIZE = 16

		/** ANSI escape introducer, the generation log is printed in red. */
		private const val ESCAPE = '\u001B'
	}
}
