package io.github.ayfri.minecraft_art.core

import io.github.ayfri.minecraft_art.ui.Bitmap
import java.util.stream.IntStream
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

data class GenerationSettings(val blocksWide: Int, val dithering: Boolean)

data class GenerationResult(
	val image: Bitmap,
	val blocksWide: Int,
	val blocksHigh: Int,
	val usage: Map<String, Int>,
	val duration: Duration,
) {
	val blockCount get() = usage.values.sum()

	/** Short human duration, the raw [Duration] prints six decimals which reads like noise in the status bar. */
	val elapsed
		get() = if (duration.inWholeMilliseconds >= 1000) duration.toString(DurationUnit.SECONDS, 2)
		else duration.toString(DurationUnit.MILLISECONDS, 1)
}

/** Turns a source image into a grid of block textures, in parallel when the dithering pass does not force an order. */
class Generator(private val palette: BlockPalette) {
	fun generate(source: Bitmap, settings: GenerationSettings, onProgress: (Float) -> Unit = {}): GenerationResult? {
		if (palette.size == 0) return null

		val start = TimeSource.Monotonic.markNow()
		val width = settings.blocksWide.coerceAtLeast(1)
		val height = (width / source.aspectRatio).toInt().coerceAtLeast(1)
		val scaled = resize(source, width, height)

		val indices = if (settings.dithering) matchDithered(scaled, width, height, onProgress)
		else matchParallel(scaled, width, height, onProgress)

		val usage = HashMap<String, Int>()
		indices.forEach { index -> if (index >= 0) usage.merge(palette.texture(index).name, 1, Int::plus) }

		val image = compose(indices, width, height)
		return GenerationResult(image, width, height, usage, start.elapsedNow())
	}

	/** Box filter downscale, averaging every source pixel of a cell gives a far better match than sampling one. */
	private fun resize(source: Bitmap, width: Int, height: Int): IntArray {
		val target = IntArray(width * height)
		IntStream.range(0, height).parallel().forEach { y ->
			val startY = (y.toLong() * source.height / height).toInt()
			val endY = maxOf(startY + 1, ((y + 1).toLong() * source.height / height).toInt())
			for (x in 0..<width) {
				val startX = (x.toLong() * source.width / width).toInt()
				val endX = maxOf(startX + 1, ((x + 1).toLong() * source.width / width).toInt())

				var alpha = 0L
				var red = 0L
				var green = 0L
				var blue = 0L
				var count = 0
				for (sourceY in startY..<endY) {
					var index = sourceY * source.width + startX
					for (sourceX in startX..<endX) {
						val pixel = source.pixels[index++]
						val pixelAlpha = pixel ushr 24
						alpha += pixelAlpha
						red += (pixel shr 16 and 0xFF) * pixelAlpha
						green += (pixel shr 8 and 0xFF) * pixelAlpha
						blue += (pixel and 0xFF) * pixelAlpha
						count++
					}
				}
				target[y * width + x] = if (alpha == 0L) 0 else {
					(alpha / count).toInt() shl 24 or ((red / alpha).toInt() shl 16) or ((green / alpha).toInt() shl 8) or (blue / alpha).toInt()
				}
			}
		}
		return target
	}

	private fun matchParallel(pixels: IntArray, width: Int, height: Int, onProgress: (Float) -> Unit): IntArray {
		val indices = IntArray(width * height)
		val done = java.util.concurrent.atomic.AtomicInteger()
		IntStream.range(0, height).parallel().forEach { y ->
			for (x in 0..<width) {
				val pixel = pixels[y * width + x]
				indices[y * width + x] = if (pixel ushr 24 < ALPHA_CUTOFF) -1
				else palette.nearest(pixel shr 16 and 0xFF, pixel shr 8 and 0xFF, pixel and 0xFF)
			}
			onProgress(done.incrementAndGet() / height.toFloat())
		}
		return indices
	}

	/** Floyd-Steinberg error diffusion, sequential by nature since each pixel pushes its error to its neighbours. */
	private fun matchDithered(pixels: IntArray, width: Int, height: Int, onProgress: (Float) -> Unit): IntArray {
		val indices = IntArray(width * height)
		val errors = FloatArray(width * height * 3)

		for (y in 0..<height) {
			for (x in 0..<width) {
				val cell = y * width + x
				val pixel = pixels[cell]
				if (pixel ushr 24 < ALPHA_CUTOFF) {
					indices[cell] = -1
					continue
				}

				val red = ((pixel shr 16 and 0xFF) + errors[cell * 3]).toInt().coerceIn(0, 255)
				val green = ((pixel shr 8 and 0xFF) + errors[cell * 3 + 1]).toInt().coerceIn(0, 255)
				val blue = ((pixel and 0xFF) + errors[cell * 3 + 2]).toInt().coerceIn(0, 255)

				val index = palette.nearest(red, green, blue)
				indices[cell] = index

				val chosen = palette.texture(index).color
				val deltaRed = (red - (chosen shr 16 and 0xFF)).toFloat()
				val deltaGreen = (green - (chosen shr 8 and 0xFF)).toFloat()
				val deltaBlue = (blue - (chosen and 0xFF)).toFloat()

				diffuse(errors, width, height, x + 1, y, deltaRed, deltaGreen, deltaBlue, 7f / 16f)
				diffuse(errors, width, height, x - 1, y + 1, deltaRed, deltaGreen, deltaBlue, 3f / 16f)
				diffuse(errors, width, height, x, y + 1, deltaRed, deltaGreen, deltaBlue, 5f / 16f)
				diffuse(errors, width, height, x + 1, y + 1, deltaRed, deltaGreen, deltaBlue, 1f / 16f)
			}
			onProgress((y + 1f) / height)
		}
		return indices
	}

	private fun diffuse(errors: FloatArray, width: Int, height: Int, x: Int, y: Int, red: Float, green: Float, blue: Float, factor: Float) {
		if (x < 0 || x >= width || y >= height) return
		val cell = (y * width + x) * 3
		errors[cell] += red * factor
		errors[cell + 1] += green * factor
		errors[cell + 2] += blue * factor
	}

	private fun compose(indices: IntArray, width: Int, height: Int): Bitmap {
		val size = BlockPalette.TEXTURE_SIZE
		val outputWidth = width * size
		val output = IntArray(outputWidth * height * size)

		IntStream.range(0, height).parallel().forEach { y ->
			for (x in 0..<width) {
				val index = indices[y * width + x]
				if (index < 0) continue

				val texture = palette.texture(index).pixels
				for (row in 0..<size) {
					val target = (y * size + row) * outputWidth + x * size
					System.arraycopy(texture, row * size, output, target, size)
				}
			}
		}
		return Bitmap(outputWidth, height * size, output)
	}

	private companion object {
		const val ALPHA_CUTOFF = 128
	}
}
