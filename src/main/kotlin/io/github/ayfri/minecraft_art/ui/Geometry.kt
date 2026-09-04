package io.github.ayfri.minecraft_art.ui

/** Mutable axis-aligned rectangle, widgets keep one instance and the layout pass rewrites it in place. */
data class Rect(var x: Float = 0f, var y: Float = 0f, var width: Float = 0f, var height: Float = 0f) {
	val right get() = x + width
	val bottom get() = y + height
	val centerX get() = x + width / 2f
	val centerY get() = y + height / 2f

	fun set(x: Float, y: Float, width: Float, height: Float) = apply {
		this.x = x
		this.y = y
		this.width = width
		this.height = height
	}

	fun set(other: Rect) = set(other.x, other.y, other.width, other.height)

	fun inset(amount: Float) = Rect(x + amount, y + amount, width - amount * 2f, height - amount * 2f)

	fun contains(px: Float, py: Float) = px >= x && px < right && py >= y && py < bottom
}

enum class Align { LEFT, CENTER, RIGHT }

enum class VerticalAlign { TOP, MIDDLE, BOTTOM }

/** Frame-rate independent approach of [target], used for every hover and press animation. */
fun approach(current: Float, target: Float, delta: Float, speed: Float = 14f): Float {
	val factor = (delta * speed).coerceIn(0f, 1f)
	return current + (target - current) * factor
}
