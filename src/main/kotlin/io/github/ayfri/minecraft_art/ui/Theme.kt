package io.github.ayfri.minecraft_art.ui

/** Single source of truth for colors, spacing and text styles, so a new backend inherits the exact same look. */
data object Theme {
	val background = 0xFF0E1014.toInt()
	val surface = 0xFF161A21.toInt()
	val surfaceRaised = 0xFF1E232C.toInt()
	val surfaceHover = 0xFF262C37.toInt()
	val border = 0xFF272D38.toInt()
	val borderStrong = 0xFF39414F.toInt()
	val text = 0xFFE9EDF5.toInt()
	val textMuted = 0xFF8B94A7.toInt()
	val textFaint = 0xFF5C6474.toInt()
	val accent = 0xFF4C93F5.toInt()
	val accentHover = 0xFF6BA6F8.toInt()
	val accentSoft = 0x334C93F5
	val success = 0xFF4FBF77.toInt()
	val warning = 0xFFE0A64B.toInt()
	val danger = 0xFFE0596B.toInt()
	val shadow = 0x55000000

	const val RADIUS = 10f
	const val RADIUS_SMALL = 6f
	const val PADDING = 16f
	const val GAP = 10f
	const val ROW_HEIGHT = 34f

	val title = TextStyle(20f, text, bold = true)
	val heading = TextStyle(12f, textFaint, bold = true)
	val body = TextStyle(14f, text)
	val bodyMuted = TextStyle(14f, textMuted)
	val small = TextStyle(12f, textMuted)
	val smallStrong = TextStyle(12f, text, bold = true)
	val mono = TextStyle(13f, textMuted)

	/** Mixes two 0xAARRGGBB colors, [amount] of 0 keeps [from] and 1 keeps [to]. */
	fun mix(from: Int, to: Int, amount: Float): Int {
		val t = amount.coerceIn(0f, 1f)
		var result = 0
		for (shift in intArrayOf(24, 16, 8, 0)) {
			val a = from ushr shift and 0xFF
			val b = to ushr shift and 0xFF
			result = result or ((a + ((b - a) * t).toInt()) shl shift)
		}
		return result
	}

	fun withAlpha(color: Int, alpha: Float) = (color and 0x00FFFFFF) or ((alpha.coerceIn(0f, 1f) * 255f).toInt() shl 24)
}
