package io.github.ayfri.minecraft_art.ui.backend

import io.github.ayfri.minecraft_art.ui.Align
import io.github.ayfri.minecraft_art.ui.Bitmap
import io.github.ayfri.minecraft_art.ui.Rect
import io.github.ayfri.minecraft_art.ui.Renderer
import io.github.ayfri.minecraft_art.ui.TextStyle
import io.github.ayfri.minecraft_art.ui.VerticalAlign
import processing.awt.PGraphicsJava2D
import processing.core.PApplet
import processing.core.PConstants
import processing.core.PFont

/**
 * Processing implementation of [Renderer]. It is the only place in the UI layer aware of Processing types, so a
 * different backend is a drop-in replacement.
 */
class ProcessingRenderer(private val sketch: PApplet) : Renderer {
	private val regular: PFont = sketch.createFont(FONT_REGULAR, FONT_RESOLUTION, true)
	private val bold: PFont = sketch.createFont(FONT_BOLD, FONT_RESOLUTION, true)
	private val blit = ScaledBlit()
	private val clips = ArrayDeque<Rect>()

	private val graphics get() = sketch.g

	override val width get() = sketch.width.toFloat()
	override val height get() = sketch.height.toFloat()

	override var delta = 1f / 60f
		private set

	/** Resets per-frame state, [elapsed] is the time since the previous frame in seconds. */
	fun beginFrame(elapsed: Float) {
		delta = elapsed.coerceIn(0.001f, 0.1f)
		clips.clear()
		graphics.noClip()
		graphics.imageMode(PConstants.CORNER)
		graphics.rectMode(PConstants.CORNER)
		graphics.ellipseMode(PConstants.CENTER)
	}

	override fun fill(rect: Rect, color: Int, radius: Float) {
		graphics.noStroke()
		graphics.fill(color)
		if (radius <= 0f) graphics.rect(rect.x, rect.y, rect.width, rect.height)
		else graphics.rect(rect.x, rect.y, rect.width, rect.height, radius)
	}

	override fun stroke(rect: Rect, color: Int, radius: Float, weight: Float) {
		graphics.noFill()
		graphics.stroke(color)
		graphics.strokeWeight(weight)
		val inset = weight / 2f
		if (radius <= 0f) graphics.rect(rect.x + inset, rect.y + inset, rect.width - weight, rect.height - weight)
		else graphics.rect(rect.x + inset, rect.y + inset, rect.width - weight, rect.height - weight, radius)
		graphics.noStroke()
	}

	override fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Int, weight: Float) {
		graphics.stroke(color)
		graphics.strokeWeight(weight)
		graphics.line(x1, y1, x2, y2)
		graphics.noStroke()
	}

	override fun circle(centerX: Float, centerY: Float, radius: Float, color: Int) {
		graphics.noStroke()
		graphics.fill(color)
		graphics.circle(centerX, centerY, radius * 2f)
	}

	override fun polygon(points: FloatArray, color: Int) {
		graphics.noStroke()
		graphics.fill(color)
		graphics.beginShape()
		var index = 0
		while (index < points.size - 1) {
			graphics.vertex(points[index], points[index + 1])
			index += 2
		}
		graphics.endShape(PConstants.CLOSE)
	}

	override fun text(value: String, x: Float, y: Float, style: TextStyle) {
		applyStyle(style)
		graphics.fill(style.color)
		graphics.text(value, x, y)
	}

	override fun textWidth(value: String, style: TextStyle): Float {
		applyStyle(style)
		return graphics.textWidth(value)
	}

	/**
	 * Bitmaps never become `PImage`s: Processing keeps its own full size `BufferedImage` copy of every one it draws,
	 * which doubles the hundreds of megabytes a dense output already holds, and then scales it single threaded.
	 */
	override fun image(bitmap: Bitmap, rect: Rect, source: Rect?, smooth: Boolean) {
		val g2 = (graphics as? PGraphicsJava2D)?.g2 ?: return
		val area = source ?: Rect(0f, 0f, bitmap.width.toFloat(), bitmap.height.toFloat())
		blit.draw(g2, bitmap, area, rect, clips.lastOrNull() ?: Rect(0f, 0f, width, height), smooth)
	}

	override fun pushClip(rect: Rect) {
		val parent = clips.lastOrNull()
		val clipped = if (parent == null) rect else Rect(
			maxOf(rect.x, parent.x),
			maxOf(rect.y, parent.y),
			minOf(rect.right, parent.right) - maxOf(rect.x, parent.x),
			minOf(rect.bottom, parent.bottom) - maxOf(rect.y, parent.y),
		)
		clips.addLast(clipped)
		graphics.clip(clipped.x, clipped.y, maxOf(0f, clipped.width), maxOf(0f, clipped.height))
	}

	override fun popClip() {
		clips.removeLastOrNull()
		val parent = clips.lastOrNull()
		if (parent == null) graphics.noClip() else graphics.clip(parent.x, parent.y, parent.width, parent.height)
	}

	private fun applyStyle(style: TextStyle) {
		graphics.textFont(if (style.bold) bold else regular, style.size)
		graphics.textAlign(
			when (style.align) {
				Align.LEFT -> PConstants.LEFT
				Align.CENTER -> PConstants.CENTER
				Align.RIGHT -> PConstants.RIGHT
			},
			when (style.baseline) {
				VerticalAlign.TOP -> PConstants.TOP
				VerticalAlign.MIDDLE -> PConstants.CENTER
				VerticalAlign.BOTTOM -> PConstants.BOTTOM
			},
		)
	}

	private companion object {
		const val FONT_REGULAR = "Segoe UI"
		const val FONT_BOLD = "Segoe UI Semibold"
		const val FONT_RESOLUTION = 48f
	}
}
