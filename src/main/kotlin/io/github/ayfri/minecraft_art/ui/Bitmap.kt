package io.github.ayfri.minecraft_art.ui

import java.awt.image.BufferedImage

/**
 * Backend-independent 32-bit ARGB image. Everything outside the renderer implementation works on these, which keeps
 * image loading, generation and export free of any toolkit type.
 */
class Bitmap(val width: Int, val height: Int, val pixels: IntArray) {
	/** Bumped whenever [pixels] changes, so a backend can tell its cached texture is stale. */
	var revision = 0
		private set

	val aspectRatio get() = width.toFloat() / height.toFloat()

	fun invalidate() {
		revision++
	}

	fun toBufferedImage() = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also {
		it.setRGB(0, 0, width, height, pixels, 0, width)
	}

	/** Nearest-neighbour downscale used for previews, where the source is always far larger than the target. */
	fun downscaled(maxSize: Int): Bitmap {
		val scale = maxSize.toFloat() / maxOf(width, height)
		if (scale >= 1f) return this

		val targetWidth = maxOf(1, (width * scale).toInt())
		val targetHeight = maxOf(1, (height * scale).toInt())
		val target = IntArray(targetWidth * targetHeight)
		for (y in 0..<targetHeight) {
			val sourceRow = (y.toLong() * height / targetHeight).toInt() * width
			val targetRow = y * targetWidth
			for (x in 0..<targetWidth) target[targetRow + x] = pixels[sourceRow + (x.toLong() * width / targetWidth).toInt()]
		}
		return Bitmap(targetWidth, targetHeight, target)
	}

	companion object {
		fun of(image: BufferedImage): Bitmap {
			val pixels = IntArray(image.width * image.height)
			image.getRGB(0, 0, image.width, image.height, pixels, 0, image.width)
			return Bitmap(image.width, image.height, pixels)
		}
	}
}
