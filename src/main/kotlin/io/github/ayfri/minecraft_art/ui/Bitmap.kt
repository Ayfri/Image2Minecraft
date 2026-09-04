package io.github.ayfri.minecraft_art.ui

import java.awt.image.BufferedImage
import java.util.stream.IntStream

/**
 * Backend-independent 32-bit ARGB image. Everything outside the renderer implementation works on these, which keeps
 * image loading, generation and export free of any toolkit type.
 */
class Bitmap(val width: Int, val height: Int, val pixels: IntArray) {
	/** Half resolution copy, built the first time a view is zoomed out far enough to need it. */
	@Volatile
	private var half: Bitmap? = null

	val aspectRatio get() = width.toFloat() / height.toFloat()

	/** Drops the mip chain, so a caller that rewrote [pixels] does not keep showing stale levels when zoomed out. */
	fun invalidate() {
		half = null
	}

	fun toBufferedImage() = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).also {
		it.setRGB(0, 0, width, height, pixels, 0, width)
	}

	/**
	 * Deepest mip level whose pixels are still worth at least half a screen pixel at [scale]. Drawing the full image
	 * at any zoom would make Java2D resample millions of pixels per frame, and sampling one pixel out of many drops
	 * detail, so a chain of averaged halves keeps a zoomed out view both fast and faithful. At [scale] `1` and above
	 * this returns the image itself, so a magnified view always shows the real pixels.
	 */
	fun levelFor(scale: Float): Bitmap {
		if (scale > 0.5f || width < 2 || height < 2) return this
		val level = half ?: halved().also { half = it }
		return level.levelFor(scale * 2f)
	}

	/** Box filtered 2x2 downscale, averaging in premultiplied alpha so transparent pixels do not bleed their colour in. */
	private fun halved(): Bitmap {
		val targetWidth = maxOf(1, width / 2)
		val targetHeight = maxOf(1, height / 2)
		val target = IntArray(targetWidth * targetHeight)
		IntStream.range(0, targetHeight).parallel().forEach { y ->
			val topRow = minOf(y * 2, height - 1) * width
			val bottomRow = minOf(y * 2 + 1, height - 1) * width
			val cells = IntArray(4)
			for (x in 0..<targetWidth) {
				val left = minOf(x * 2, width - 1)
				val right = minOf(x * 2 + 1, width - 1)
				cells[0] = topRow + left
				cells[1] = topRow + right
				cells[2] = bottomRow + left
				cells[3] = bottomRow + right
				var alpha = 0
				var red = 0
				var green = 0
				var blue = 0
				for (cell in cells) {
					val pixel = pixels[cell]
					val pixelAlpha = pixel ushr 24
					alpha += pixelAlpha
					red += (pixel shr 16 and 0xFF) * pixelAlpha
					green += (pixel shr 8 and 0xFF) * pixelAlpha
					blue += (pixel and 0xFF) * pixelAlpha
				}
				target[y * targetWidth + x] =
					if (alpha == 0) 0 else (alpha / 4) shl 24 or (red / alpha shl 16) or (green / alpha shl 8) or (blue / alpha)
			}
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
