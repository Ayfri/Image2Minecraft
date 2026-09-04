package io.github.ayfri.minecraft_art.core

import io.github.ayfri.minecraft_art.ui.Bitmap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicIntegerArray
import java.util.stream.IntStream
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.TimeSource

data class GenerationSettings(val blocksWide: Int, val dithering: Boolean) {
	companion object {
		/** Blocks per side the UI offers at most. */
		const val MAX_BLOCKS_SIDE = 4000

		/** One 16x16 block texture, composed into the output as plain ARGB. */
		private const val PIXELS_PER_BLOCK = 256L

		/** The output array, its preview mip chain and the backend texture are all live at once while a result is shown. */
		private const val BYTES_PER_BLOCK = PIXELS_PER_BLOCK * Int.SIZE_BYTES * 3

		/**
		 * Total blocks one generation may produce. [MAX_BLOCKS_SIDE] is not reachable on its own, a full square that
		 * wide composes to 64000 x 64000 pixels and no `IntArray` can hold that many, so the heap sets the real bound.
		 */
		val MAX_BLOCKS = (Runtime.getRuntime().maxMemory() * 2 / 5 / BYTES_PER_BLOCK).toInt()
	}
}

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

		val image = compose(indices, width, height)
		return GenerationResult(image, width, height, count(indices), start.elapsedNow())
	}

	/** Tally per palette index first, hashing a block name once per cell would dominate the whole generation. */
	private fun count(indices: IntArray): Map<String, Int> {
		val counts = IntArray(palette.size)
		for (index in indices) if (index >= 0) counts[index]++
		return buildMap {
			counts.forEachIndexed { index, count -> if (count > 0) put(palette.texture(index).name, count) }
		}
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
		val done = AtomicInteger()
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

	/**
	 * Floyd-Steinberg error diffusion, spread over a row wavefront. Each cell only needs the row above to be two
	 * columns ahead, so a thread per row can run one behind the previous one and still consume the exact same errors
	 * in the exact same order as a sequential pass. Threads are plain platform threads because a worker spins waiting
	 * on its predecessor, which would deadlock on a pool that runs fewer rows at once than there are workers.
	 */
	private fun matchDithered(pixels: IntArray, width: Int, height: Int, onProgress: (Float) -> Unit): IntArray {
		val indices = IntArray(width * height)
		val errors = FloatArray(width * height * 3)
		val progress = AtomicIntegerArray(height)
		val done = AtomicInteger()
		val workers = minOf(WAVEFRONT_WORKERS, height)

		val threads = List(workers) { worker ->
			Thread.ofPlatform().name("dither-$worker").start {
				var y = worker
				while (y < height) {
					var x = 0
					while (x < width) {
						val end = minOf(x + WAVEFRONT_CHUNK, width)
						/** The chunk reads the errors the row above pushes down from its column [end], one past the chunk. */
						if (y > 0) {
							val needed = minOf(width, end + 1)
							while (progress.get(y - 1) < needed) Thread.onSpinWait()
						}
						ditherRow(pixels, indices, errors, width, height, y, x, end)
						progress.set(y, end)
						x = end
					}
					onProgress(done.incrementAndGet() / height.toFloat())
					y += workers
				}
			}
		}
		threads.forEach(Thread::join)
		return indices
	}

	/** Columns [from] until [to] of one row, the sequential core of the dithering pass. */
	private fun ditherRow(pixels: IntArray, indices: IntArray, errors: FloatArray, width: Int, height: Int, y: Int, from: Int, to: Int) {
		for (x in from..<to) {
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

		/** One task per output pixel row, so every copy of a row advances along [output] instead of jumping by a full row. */
		IntStream.range(0, height * size).parallel().forEach { outputRow ->
			val row = (outputRow % size) * size
			var target = outputRow * outputWidth
			val cell = (outputRow / size) * width
			for (x in 0..<width) {
				val index = indices[cell + x]
				if (index >= 0) System.arraycopy(palette.texture(index).pixels, row, output, target, size)
				target += size
			}
		}
		return Bitmap(outputWidth, height * size, output)
	}

	private companion object {
		const val ALPHA_CUTOFF = 128

		/** Columns a dithering worker runs before publishing its progress, one publication per column would thrash the cache line. */
		const val WAVEFRONT_CHUNK = 32

		/** A waiting worker spins rather than parking, so the wavefront needs spare cores or the rows it waits on get starved. */
		val WAVEFRONT_WORKERS = (Runtime.getRuntime().availableProcessors() * 2 / 3).coerceAtLeast(1)
	}
}
