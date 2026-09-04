package io.github.ayfri.minecraft_art.bench

import io.github.ayfri.minecraft_art.core.AppPaths
import io.github.ayfri.minecraft_art.core.BlockPalette
import io.github.ayfri.minecraft_art.core.GenerationResult
import io.github.ayfri.minecraft_art.core.GenerationSettings
import io.github.ayfri.minecraft_art.core.Generator
import io.github.ayfri.minecraft_art.ui.Bitmap
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.math.sqrt
import kotlin.system.exitProcess
import kotlin.time.TimeSource

/** Deterministic source image, a smooth gradient crossed with a hash based noise so the palette is exercised widely. */
private fun sourceImage(width: Int, height: Int): Bitmap {
	val pixels = IntArray(width * height)
	for (y in 0..<height) {
		for (x in 0..<width) {
			var hash = (x * 374761393 + y * 668265263) xor (x * y + 1)
			hash = (hash xor (hash ushr 13)) * -1028477387
			hash = hash xor (hash ushr 16)
			val red = (x * 255 / width + (hash and 0x3F)) and 0xFF
			val green = (y * 255 / height + (hash ushr 6 and 0x3F)) and 0xFF
			val blue = ((x + y) * 255 / (width + height) + (hash ushr 12 and 0x3F)) and 0xFF
			pixels[y * width + x] = 0xFF shl 24 or (red shl 16) or (green shl 8) or blue
		}
	}
	return Bitmap(width, height, pixels)
}

private fun palette(): BlockPalette {
	val version = AppPaths.textures.takeIf { it.isDirectory() }?.listDirectoryEntries()?.lastOrNull { it.isDirectory() }
	if (version == null) {
		println("No extracted textures under ${AppPaths.textures}, run the app once to build a palette.")
		exitProcess(1)
	}
	return BlockPalette().also { println("Palette ${version.fileName} : ${it.load(version.toFile())} faces") }
}

/** Milliseconds of every sample, reported as mean and standard deviation since a single run hides JIT noise. */
private fun measure(warmups: Int, samples: Int, block: () -> Unit): String {
	repeat(warmups) { block() }
	val times = DoubleArray(samples) {
		val start = TimeSource.Monotonic.markNow()
		block()
		start.elapsedNow().inWholeMicroseconds / 1000.0
	}
	val mean = times.average()
	val deviation = sqrt(times.sumOf { (it - mean) * (it - mean) } / samples)
	return "%8.1f ms  +- %.1f".format(mean, deviation)
}

/** Order independent digest of a result, printed so an optimization can be proven to keep the exact same output. */
private fun digest(result: GenerationResult): String {
	var hash = 1125899906842597L
	for (pixel in result.image.pixels) hash = hash * 31 + pixel
	for ((name, count) in result.usage.entries.sortedBy { it.key }) hash = hash * 31 + name.hashCode() + count
	return hash.toULong().toString(16).padStart(16, '0')
}

fun main() {
	val palette = palette()
	val generator = Generator(palette)
	val source = sourceImage(2048, 2048)

	for (blocks in listOf(256, 512, 1024)) {
		for (dithering in listOf(false, true)) {
			val settings = GenerationSettings(blocks, dithering)
			val label = "$blocks blocks ${if (dithering) "dithered" else "plain   "}"
			val digest = digest(generator.generate(source, settings)!!)
			println("$label ${measure(warmups = 3, samples = 10) { generator.generate(source, settings) }}  $digest")
		}
	}
}
