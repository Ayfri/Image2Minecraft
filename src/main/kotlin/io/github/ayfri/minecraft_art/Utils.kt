package io.github.ayfri.minecraft_art

import processing.core.PGraphics
import processing.core.PImage
import processing.core.PVector

/** Returns the block texture whose average color is closest to [target], and counts that block in [Blocks.used]. */
fun getNearestResizedBlock(graphics: PGraphics, target: Int): PImage? {
	var min = Float.MAX_VALUE
	var nearestName: String? = null
	var nearest: PImage? = null

	for ((name, block) in Blocks.textures) {
		val diff = getColorDistance(graphics, Blocks.colors.getValue(name), target)
		if (diff >= min) continue

		min = diff
		nearestName = name
		nearest = block
	}

	nearestName?.let(Blocks::countUse)
	return nearest
}

fun getColorDistance(graphics: PGraphics, a: Int, b: Int): Float {
	val vector = PVector(graphics.red(a), graphics.green(a), graphics.blue(a))
	vector.sub(graphics.red(b), graphics.green(b), graphics.blue(b))
	return vector.mag()
}

fun getAverageColor(image: PImage): Int {
	image.loadPixels()
	val pixels = image.pixels
	var red = 0
	var green = 0
	var blue = 0

	for (pixel in pixels) {
		red += pixel shr 16 and 0xFF
		green += pixel shr 8 and 0xFF
		blue += pixel and 0xFF
	}

	return (red / pixels.size shl 16) or (green / pixels.size shl 8) or (blue / pixels.size)
}

fun isInRange(value: Int, min: Int, max: Int) = value > min && value < max
