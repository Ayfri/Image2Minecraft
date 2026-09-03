package io.github.ayfri.minecraft_art

import processing.core.PImage
import java.io.File
import java.io.FileNotFoundException

// todo Exceptions gérées par le JSON
data class TextureManager(private val sketch: Main) {
	/** Registers every flat, opaque 16x16 texture of the local `blocks` directory into [Blocks]. */
	fun init() {
		val files = getLocalDirectory("blocks")?.listFiles() ?: return

		for (blockFile in files) {
			val name = blockFile.name
			if (!name.endsWith(".png") || EXCEPTIONS.any { it in name }) continue

			val image = getImage(blockFile)
			image.loadPixels()
			if (image.width != 16 || image.height != 16) continue

			val averageColor = getAverageColor(image)
			/** A texture is kept only when fully opaque and flat enough, so the block reads as a single solid color once scaled down. */
			val isFlatAndOpaque = image.pixels.all {
				getColorDistance(sketch.g, it, averageColor) <= 60 && sketch.alpha(it) >= 255
			}
			if (!isFlatAndOpaque) continue

			Blocks.register(name, image, averageColor)
		}
	}

	fun getImage(file: File): PImage {
		if (!file.exists()) throw FileNotFoundException("File ${file.path} not found.")
		return sketch.loadImage(file.path)
	}

	companion object {
		val EXCEPTIONS = listOf("top", "bottom", "brewing_stand", "cauldron", "hopper", "slab", "shulker", "anvil", "wheat")
	}
}
