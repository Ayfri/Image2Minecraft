package io.github.ayfri.minecraft_art.core

import kotlin.math.pow

/** sRGB byte to linear light, looked up instead of computed since every input is already a 0..255 channel. */
private val LINEAR = FloatArray(256) { index ->
	val channel = index / 255f
	if (channel <= 0.04045f) channel / 12.92f else ((channel + 0.055f) / 1.055f).pow(2.4f)
}

/**
 * Converts a sRGB color to Oklab and writes the L, a, b components at [offset]. Matching blocks in Oklab instead of
 * raw RGB keeps dark and saturated areas from collapsing onto the same block.
 */
fun oklab(red: Int, green: Int, blue: Int, out: FloatArray, offset: Int = 0) {
	val r = LINEAR[red]
	val g = LINEAR[green]
	val b = LINEAR[blue]

	val l = Math.cbrt((0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b).toDouble()).toFloat()
	val m = Math.cbrt((0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b).toDouble()).toFloat()
	val s = Math.cbrt((0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b).toDouble()).toFloat()

	out[offset] = 0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s
	out[offset + 1] = 1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s
	out[offset + 2] = 0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s
}

/** Squared RGB distance, only used to measure how flat a texture is. */
fun rgbDistanceSquared(a: Int, b: Int): Int {
	val deltaRed = (a shr 16 and 0xFF) - (b shr 16 and 0xFF)
	val deltaGreen = (a shr 8 and 0xFF) - (b shr 8 and 0xFF)
	val deltaBlue = (a and 0xFF) - (b and 0xFF)
	return deltaRed * deltaRed + deltaGreen * deltaGreen + deltaBlue * deltaBlue
}

fun rgb(red: Int, green: Int, blue: Int) = 0xFF shl 24 or (red shl 16) or (green shl 8) or blue
