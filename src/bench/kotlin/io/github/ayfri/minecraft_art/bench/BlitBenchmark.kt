package io.github.ayfri.minecraft_art.bench

import io.github.ayfri.minecraft_art.ui.Bitmap
import io.github.ayfri.minecraft_art.ui.Rect
import io.github.ayfri.minecraft_art.ui.backend.ScaledBlit
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.awt.image.DirectColorModel
import java.awt.image.Raster
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sqrt
import kotlin.time.TimeSource

private const val W = 620
private const val H = 860

private fun sample(warmups: Int, samples: Int, block: () -> Unit): String {
	repeat(warmups) { block() }
	val times = DoubleArray(samples) {
		val start = TimeSource.Monotonic.markNow()
		block()
		start.elapsedNow().inWholeMicroseconds / 1000.0
	}
	val mean = times.average()
	return "%7.2f ms +- %.2f".format(mean, sqrt(times.sumOf { (it - mean) * (it - mean) } / samples))
}

private fun wrapped(width: Int, height: Int, pixels: IntArray): BufferedImage {
	val model = DirectColorModel(32, 0x00FF0000, 0x0000FF00, 0x000000FF, 0xFF000000.toInt())
	val raster = Raster.createWritableRaster(model.createCompatibleSampleModel(width, height), DataBufferInt(pixels, width * height), null)
	return BufferedImage(model, raster, false, null)
}

/** A block output is flat coloured 16 px tiles, so neighbouring pixels are far more correlated than pure noise. */
private fun blocks(width: Int, height: Int): Bitmap {
	val pixels = IntArray(width * height)
	for (y in 0..<height) {
		for (x in 0..<width) {
			var hash = (x / 16) * 374761393 + (y / 16) * 668265263
			hash = (hash xor (hash ushr 13)) * -1028477387
			pixels[y * width + x] = 0xFF shl 24 or (hash ushr 8 and 0xFFFFFF)
		}
	}
	return Bitmap(width, height, pixels)
}

private fun difference(left: IntArray, right: IntArray): String {
	var total = 0L
	for (index in left.indices) {
		for (shift in intArrayOf(16, 8, 0)) total += abs((left[index] shr shift and 0xFF) - (right[index] shr shift and 0xFF))
	}
	return "%.2f / 255".format(total.toDouble() / left.size / 3)
}

fun main() {
	val pane = IntArray(W * H)
	val paneImage = wrapped(W, H, pane)
	val g2 = paneImage.createGraphics()
	val blit = ScaledBlit()

	for (scale in listOf(0.55f, 0.83f, 1f, 3f, 12f)) {
		val columns = minOf(4096, ceil(W / scale).toInt())
		val rows = minOf(4096, ceil(H / scale).toInt())
		val bitmap = blocks(columns, rows)
		val image = wrapped(columns, rows, bitmap.pixels)
		val source = Rect(0f, 0f, columns.toFloat(), rows.toFloat())
		val destination = Rect(0f, 0f, columns * scale, rows * scale)
		val clip = Rect(0f, 0f, W.toFloat(), H.toFloat())
		val smooth = scale < 1f
		println("\n== scale %.2f, source $columns x $rows, %s".format(scale, if (smooth) "smoothed" else "crisp"))

		g2.setRenderingHint(
			RenderingHints.KEY_INTERPOLATION,
			if (smooth) RenderingHints.VALUE_INTERPOLATION_BILINEAR else RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR,
		)
		val java2d = { g2.drawImage(image, 0, 0, destination.width.toInt(), destination.height.toInt(), 0, 0, columns, rows, null); Unit }
		println("  java2d scaled blit    " + sample(5, 30, java2d))
		java2d()
		val reference = pane.copyOf()

		println("  ScaledBlit            " + sample(5, 30) { blit.draw(g2, bitmap, source, destination, clip, smooth) })
		blit.draw(g2, bitmap, source, destination, clip, smooth)
		println("  mean channel delta    " + difference(reference, pane))
	}
	g2.dispose()

	println("")
	println("== mip chain build, the cost a generation now pays on its worker instead of the draw thread")
	for (side in listOf(3000, 6000, 12000)) {
		val bitmap = blocks(side, side)
		println("  $side x $side px" + sample(1, 5) { bitmap.invalidate(); bitmap.buildLevels() })
	}
}
