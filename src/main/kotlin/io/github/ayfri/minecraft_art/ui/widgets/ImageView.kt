package io.github.ayfri.minecraft_art.ui.widgets

import io.github.ayfri.minecraft_art.ui.Align
import io.github.ayfri.minecraft_art.ui.Bitmap
import io.github.ayfri.minecraft_art.ui.Icon
import io.github.ayfri.minecraft_art.ui.PointerAction
import io.github.ayfri.minecraft_art.ui.PointerEvent
import io.github.ayfri.minecraft_art.ui.Rect
import io.github.ayfri.minecraft_art.ui.Renderer
import io.github.ayfri.minecraft_art.ui.Theme
import io.github.ayfri.minecraft_art.ui.VerticalAlign
import io.github.ayfri.minecraft_art.ui.Widget
import io.github.ayfri.minecraft_art.ui.icon
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt

/** Zoomable and pannable image pane with a checkerboard background, block grid overlay and an empty state. */
class ImageView(private val emptyHint: String) : Widget() {
	var bitmap: Bitmap? = null
		set(value) {
			val changed = field !== value
			field = value
			if (changed) fit()
		}

	/** Size in image pixels of one grid cell, 0 hides the overlay entirely. */
	var gridSize = 0
	var showGrid = false

	private var zoom = 1f
	private var centerU = 0.5f
	private var centerV = 0.5f
	private var dragX = 0f
	private var dragY = 0f
	private var panning = false
	private var checker: Bitmap? = null

	override val capturing get() = panning

	val zoomPercent: Int
		get() {
			val image = bitmap ?: return 0
			return (fitScale(image) * zoom * 100f).roundToInt()
		}

	fun fit() {
		zoom = 1f
		centerU = 0.5f
		centerV = 0.5f
	}

	fun zoomToPixels() {
		val image = bitmap ?: return
		zoom = 1f / fitScale(image)
	}

	private fun fitScale(image: Bitmap): Float {
		val area = bounds.inset(1f)
		return minOf(area.width / image.width, area.height / image.height)
	}

	private fun frame(image: Bitmap): Rect {
		val area = bounds.inset(1f)
		val scale = fitScale(image) * zoom
		val width = image.width * scale
		val height = image.height * scale
		return Rect(area.centerX - centerU * width, area.centerY - centerV * height, width, height)
	}

	override fun render(renderer: Renderer) {
		renderer.fill(bounds, Theme.background, Theme.RADIUS)
		renderer.stroke(bounds, Theme.border, Theme.RADIUS)

		val image = bitmap
		if (image == null) {
			renderer.icon(Icon.IMAGE, bounds.centerX, bounds.centerY - 16f, 34f, Theme.textFaint)
			renderer.text(emptyHint, bounds.centerX, bounds.centerY + 22f, Theme.bodyMuted.copy(align = Align.CENTER, color = Theme.textFaint))
			return
		}

		val area = bounds.inset(1f)
		renderer.pushClip(area)
		val target = frame(image)
		checkerboard(renderer, target)
		draw(renderer, image, target)
		if (showGrid && gridSize > 0) grid(renderer, target, image)
		renderer.popClip()

		val badge = "${zoomPercent}%"
		val badgeWidth = renderer.textWidth(badge, Theme.small) + 16f
		renderer.fill(Rect(bounds.right - badgeWidth - 8f, bounds.bottom - 26f, badgeWidth, 18f), Theme.withAlpha(Theme.surface, 0.85f), 9f)
		renderer.text(badge, bounds.right - 16f, bounds.bottom - 17f, Theme.small.copy(align = Align.RIGHT, baseline = VerticalAlign.MIDDLE))
	}

	/**
	 * Draws only the visible part of the image, taken from the mip level matching the on-screen scale. A full block
	 * output is tens of megapixels, so handing the whole thing to the backend every frame would resample far more
	 * pixels than the pane can show, while the level choice keeps every block texture intact once zoomed in to 1:1.
	 */
	private fun draw(renderer: Renderer, image: Bitmap, target: Rect) {
		val area = bounds.inset(1f)
		val left = maxOf(area.x, target.x)
		val top = maxOf(area.y, target.y)
		val right = minOf(area.right, target.right)
		val bottom = minOf(area.bottom, target.bottom)
		if (right <= left || bottom <= top) return

		val level = image.levelFor(target.width / image.width)
		val scale = target.width / level.width
		val sourceX = floor((left - target.x) / scale).toInt().coerceIn(0, level.width - 1)
		val sourceY = floor((top - target.y) / scale).toInt().coerceIn(0, level.height - 1)
		val sourceRight = ceil((right - target.x) / scale).toInt().coerceIn(sourceX + 1, level.width)
		val sourceBottom = ceil((bottom - target.y) / scale).toInt().coerceIn(sourceY + 1, level.height)

		val visible = Rect(
			target.x + sourceX * scale,
			target.y + sourceY * scale,
			(sourceRight - sourceX) * scale,
			(sourceBottom - sourceY) * scale,
		)
		val source = Rect(sourceX.toFloat(), sourceY.toFloat(), (sourceRight - sourceX).toFloat(), (sourceBottom - sourceY).toFloat())
		/** Pixel art must stay crisp once magnified, smoothing only helps when the level is still shown smaller than 1:1. */
		renderer.image(level, visible, source, smooth = scale < 1f)
	}

	/** The checkerboard is a tiny bitmap of one pixel per cell blown up by the renderer, so it costs a single draw. */
	private fun checkerboard(renderer: Renderer, target: Rect) {
		val columns = maxOf(1, (target.width / CHECKER_CELL).toInt() + 1)
		val rows = maxOf(1, (target.height / CHECKER_CELL).toInt() + 1)
		val cached = checker
		val pattern = if (cached != null && cached.width == columns && cached.height == rows) cached else {
			Bitmap(columns, rows, IntArray(columns * rows) { if ((it / columns + it % columns) % 2 == 0) 0xFF15181E.toInt() else 0xFF1B1F27.toInt() })
				.also { checker = it }
		}
		renderer.image(pattern, Rect(target.x, target.y, columns * CHECKER_CELL, rows * CHECKER_CELL), smooth = false)
	}

	private fun grid(renderer: Renderer, target: Rect, image: Bitmap) {
		val step = target.width / image.width * gridSize
		if (step < 9f) return

		val color = Theme.withAlpha(0xFFFFFFFF.toInt(), 0.12f)
		var x = target.x
		while (x <= target.right) {
			renderer.line(x, maxOf(target.y, bounds.y), x, minOf(target.bottom, bounds.bottom), color)
			x += step
		}
		var y = target.y
		while (y <= target.bottom) {
			renderer.line(maxOf(target.x, bounds.x), y, minOf(target.right, bounds.right), y, color)
			y += step
		}
	}

	override fun onPointer(event: PointerEvent): Boolean {
		val image = bitmap ?: return false

		when (event.action) {
			PointerAction.WHEEL -> {
				if (!hovered) return false
				val target = frame(image)
				val u = (event.x - target.x) / target.width
				val v = (event.y - target.y) / target.height
				zoom = (zoom * 1.15f.pow(-event.scroll)).coerceIn(0.2f, 60f)
				val next = frame(image)
				val area = bounds.inset(1f)
				centerU = ((area.centerX - event.x) / next.width + u).coerceIn(-0.25f, 1.25f)
				centerV = ((area.centerY - event.y) / next.height + v).coerceIn(-0.25f, 1.25f)
				return true
			}

			PointerAction.PRESS -> {
				if (event.clickCount >= 2) fit()
				panning = true
				dragX = event.x
				dragY = event.y
				return true
			}

			PointerAction.DRAG -> {
				if (!panning) return false
				val target = frame(image)
				centerU = (centerU - (event.x - dragX) / target.width).coerceIn(-0.25f, 1.25f)
				centerV = (centerV - (event.y - dragY) / target.height).coerceIn(-0.25f, 1.25f)
				dragX = event.x
				dragY = event.y
				return true
			}

			PointerAction.RELEASE -> {
				val wasPanning = panning
				panning = false
				return wasPanning
			}

			else -> return false
		}
	}

	private companion object {
		const val CHECKER_CELL = 12f
	}
}
