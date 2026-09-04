package io.github.ayfri.minecraft_art.ui

/** Immutable text run description, kept separate from [Renderer] so a backend can cache measurements per style. */
data class TextStyle(
	val size: Float,
	val color: Int,
	val bold: Boolean = false,
	val align: Align = Align.LEFT,
	val baseline: VerticalAlign = VerticalAlign.MIDDLE,
)

/**
 * Backend-agnostic immediate-mode drawing surface. The whole widget layer talks only to this interface, so swapping
 * Processing for a faster backend (Skia, OpenGL, Compose) means writing one new implementation and nothing else.
 */
interface Renderer {
	val width: Float
	val height: Float

	/** Seconds elapsed since the previous frame, widgets use it to keep animations frame-rate independent. */
	val delta: Float

	fun fill(rect: Rect, color: Int, radius: Float = 0f)
	fun stroke(rect: Rect, color: Int, radius: Float = 0f, weight: Float = 1f)
	fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Int, weight: Float = 1f)
	fun circle(centerX: Float, centerY: Float, radius: Float, color: Int)
	fun polygon(points: FloatArray, color: Int)
	fun text(value: String, x: Float, y: Float, style: TextStyle)
	fun textWidth(value: String, style: TextStyle): Float
	/** Draws [bitmap] into [rect], restricted to the pixel region [source] when given so a huge image only costs its visible part. */
	fun image(bitmap: Bitmap, rect: Rect, source: Rect? = null, smooth: Boolean = true)
	fun pushClip(rect: Rect)
	fun popClip()

	/** Draws [value] truncated with an ellipsis so it never exceeds [maxWidth]. */
	fun textEllipsis(value: String, x: Float, y: Float, maxWidth: Float, style: TextStyle) {
		if (textWidth(value, style) <= maxWidth) return text(value, x, y, style)

		var candidate = value
		while (candidate.length > 1 && textWidth("$candidate…", style) > maxWidth) candidate = candidate.dropLast(1)
		text("$candidate…", x, y, style)
	}
}
