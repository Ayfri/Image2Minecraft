package io.github.ayfri.minecraft_art.ui.backend

import io.github.ayfri.minecraft_art.ui.Bitmap
import io.github.ayfri.minecraft_art.ui.Rect
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.awt.image.DirectColorModel
import java.awt.image.Raster
import java.util.stream.IntStream
import kotlin.math.ceil
import kotlin.math.floor

/**
 * Draws a region of a [Bitmap] into a rectangle by resampling it into a screen sized buffer, then blitting that buffer
 * one to one. Java2D scales images in a single threaded loop that costs around 8 ms for a 620x860 pane, which two image
 * panes alone turn into a missed frame, while an inverse mapping spread over every core does the same work in under
 * half a millisecond. Only the pixels the clip actually shows are ever touched, so zooming in stays flat in cost.
 */
class ScaledBlit {
	private var pixels = IntArray(0)
	private var image: BufferedImage? = null

	fun draw(g2: Graphics2D, bitmap: Bitmap, source: Rect, destination: Rect, clip: Rect, smooth: Boolean) {
		val left = maxOf(destination.x, clip.x)
		val top = maxOf(destination.y, clip.y)
		val right = minOf(destination.right, clip.right)
		val bottom = minOf(destination.bottom, clip.bottom)

		val x = floor(left).toInt()
		val y = floor(top).toInt()
		val width = ceil(right).toInt() - x
		val height = ceil(bottom).toInt() - y
		if (width <= 0 || height <= 0 || destination.width <= 0f || destination.height <= 0f) return

		val target = buffer(width, height)
		val scaleX = source.width / destination.width
		val scaleY = source.height / destination.height
		/** Dest pixel centres map back through the untruncated rectangle, so panning by a fraction of a pixel never jitters. */
		val originX = source.x + (x + 0.5f - destination.x) * scaleX
		val originY = source.y + (y + 0.5f - destination.y) * scaleY

		if (smooth) bilinear(bitmap, originX - 0.5f, originY - 0.5f, scaleX, scaleY, width, height)
		else nearest(bitmap, originX, originY, scaleX, scaleY, width, height)
		g2.drawImage(target, x, y, null)
	}

	private fun nearest(bitmap: Bitmap, originX: Float, originY: Float, scaleX: Float, scaleY: Float, width: Int, height: Int) {
		val columns = IntArray(width) { (originX + it * scaleX).toInt().coerceIn(0, bitmap.width - 1) }
		IntStream.range(0, height).parallel().forEach { row ->
			val line = (originY + row * scaleY).toInt().coerceIn(0, bitmap.height - 1) * bitmap.width
			val out = row * width
			for (column in 0..<width) pixels[out + column] = bitmap.pixels[line + columns[column]]
		}
	}

	/** Averages in premultiplied alpha, matching [Bitmap] mip levels so a transparent neighbour never bleeds its colour in. */
	private fun bilinear(bitmap: Bitmap, originX: Float, originY: Float, scaleX: Float, scaleY: Float, width: Int, height: Int) {
		val lasts = bitmap.width - 1
		val columns = IntArray(width)
		val weights = IntArray(width)
		for (column in 0..<width) {
			val u = originX + column * scaleX
			val left = floor(u).toInt()
			columns[column] = left.coerceIn(0, lasts)
			weights[column] = if (left < 0 || left >= lasts) 0 else ((u - left) * 256f).toInt()
		}

		IntStream.range(0, height).parallel().forEach { row ->
			val v = originY + row * scaleY
			val top = floor(v).toInt()
			val weightY = if (top < 0 || top >= bitmap.height - 1) 0 else ((v - top) * 256f).toInt()
			val topRow = top.coerceIn(0, bitmap.height - 1) * bitmap.width
			val bottomRow = topRow + if (weightY == 0) 0 else bitmap.width
			val out = row * width
			for (column in 0..<width) {
				val left = columns[column]
				val right = left + if (weights[column] == 0) 0 else 1
				pixels[out + column] = mix(
					bitmap.pixels[topRow + left],
					bitmap.pixels[topRow + right],
					bitmap.pixels[bottomRow + left],
					bitmap.pixels[bottomRow + right],
					weights[column],
					weightY,
				)
			}
		}
	}

	private fun mix(topLeft: Int, topRight: Int, bottomLeft: Int, bottomRight: Int, weightX: Int, weightY: Int): Int {
		/** Weights are kept on 256 with the first one taking the rounding slack, so the four always sum to exactly one. */
		val rightTop = weightX * (256 - weightY) shr 8
		val leftBottom = (256 - weightX) * weightY shr 8
		val rightBottom = weightX * weightY shr 8

		val leftTop = 256 - rightTop - leftBottom - rightBottom

		/** Three integer divisions per pixel dominate the loop, and every opaque source skips them, which is nearly all of them. */
		if (topLeft and topRight and bottomLeft and bottomRight ushr 24 == 0xFF) {
			val opaqueRed = (topLeft shr 16 and 0xFF) * leftTop + (topRight shr 16 and 0xFF) * rightTop +
				(bottomLeft shr 16 and 0xFF) * leftBottom + (bottomRight shr 16 and 0xFF) * rightBottom
			val opaqueGreen = (topLeft shr 8 and 0xFF) * leftTop + (topRight shr 8 and 0xFF) * rightTop +
				(bottomLeft shr 8 and 0xFF) * leftBottom + (bottomRight shr 8 and 0xFF) * rightBottom
			val opaqueBlue = (topLeft and 0xFF) * leftTop + (topRight and 0xFF) * rightTop +
				(bottomLeft and 0xFF) * leftBottom + (bottomRight and 0xFF) * rightBottom
			return 0xFF shl 24 or (opaqueRed shl 8 and 0xFF0000) or (opaqueGreen and 0xFF00) or (opaqueBlue shr 8)
		}

		val alphaTopLeft = (topLeft ushr 24) * leftTop
		val alphaTopRight = (topRight ushr 24) * rightTop
		val alphaBottomLeft = (bottomLeft ushr 24) * leftBottom
		val alphaBottomRight = (bottomRight ushr 24) * rightBottom
		val alpha = alphaTopLeft + alphaTopRight + alphaBottomLeft + alphaBottomRight
		if (alpha == 0) return 0

		val red = (topLeft shr 16 and 0xFF) * alphaTopLeft + (topRight shr 16 and 0xFF) * alphaTopRight +
			(bottomLeft shr 16 and 0xFF) * alphaBottomLeft + (bottomRight shr 16 and 0xFF) * alphaBottomRight
		val green = (topLeft shr 8 and 0xFF) * alphaTopLeft + (topRight shr 8 and 0xFF) * alphaTopRight +
			(bottomLeft shr 8 and 0xFF) * alphaBottomLeft + (bottomRight shr 8 and 0xFF) * alphaBottomRight
		val blue = (topLeft and 0xFF) * alphaTopLeft + (topRight and 0xFF) * alphaTopRight +
			(bottomLeft and 0xFF) * alphaBottomLeft + (bottomRight and 0xFF) * alphaBottomRight
		return (alpha shr 8) shl 24 or (red / alpha shl 16) or (green / alpha shl 8) or (blue / alpha)
	}

	/** The array is grown to the largest pane seen and the image rebound only when the visible size changes. */
	private fun buffer(width: Int, height: Int): BufferedImage {
		val cached = image
		if (cached != null && cached.width == width && cached.height == height) return cached
		if (pixels.size < width * height) pixels = IntArray(width * height)

		val model = DirectColorModel(32, 0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000.toInt())
		val raster = Raster.createWritableRaster(model.createCompatibleSampleModel(width, height), DataBufferInt(pixels, width * height), null)
		return BufferedImage(model, raster, false, null).also { image = it }
	}
}
