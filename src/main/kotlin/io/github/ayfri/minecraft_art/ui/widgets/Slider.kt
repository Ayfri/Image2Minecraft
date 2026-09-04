package io.github.ayfri.minecraft_art.ui.widgets

import io.github.ayfri.minecraft_art.ui.Align
import io.github.ayfri.minecraft_art.ui.PointerAction
import io.github.ayfri.minecraft_art.ui.PointerEvent
import io.github.ayfri.minecraft_art.ui.Rect
import io.github.ayfri.minecraft_art.ui.Renderer
import io.github.ayfri.minecraft_art.ui.Theme
import io.github.ayfri.minecraft_art.ui.Widget
import kotlin.math.roundToInt

/** Labelled slider, the label row sits above the track and the value is shown on the right of it. */
class Slider(
	private val label: String,
	private val min: Float,
	private val max: Float,
	value: Float,
	private val step: Float = 1f,
	private val format: (Float) -> String = { it.roundToInt().toString() },
	private val onChange: (Float) -> Unit = {},
) : Widget() {
	var value = value
		set(newValue) {
			val snapped = snap(newValue)
			if (snapped == field) return
			field = snapped
			onChange(snapped)
		}

	override val capturing get() = held

	private val track get() = Rect(bounds.x, bounds.bottom - 14f, bounds.width, 6f)
	private val progress get() = ((value - min) / (max - min)).coerceIn(0f, 1f)

	private fun snap(raw: Float) = (Math.round(raw / step) * step).coerceIn(min, max)

	override fun render(renderer: Renderer) {
		val color = if (enabled) Theme.text else Theme.textFaint
		renderer.text(label, bounds.x, bounds.y + 8f, Theme.small.copy(color = Theme.textMuted))
		renderer.text(format(value), bounds.right, bounds.y + 8f, Theme.smallStrong.copy(color = color, align = Align.RIGHT))

		val line = track
		renderer.fill(line, Theme.surfaceHover, 3f)
		if (progress > 0f) renderer.fill(Rect(line.x, line.y, line.width * progress, line.height), Theme.accent, 3f)

		val handleX = line.x + line.width * progress
		val radius = 7f + hoverAmount * 1.5f + pressAmount
		renderer.circle(handleX, line.centerY, radius + 3f, Theme.withAlpha(Theme.accent, 0.18f * (hoverAmount + pressAmount)))
		renderer.circle(handleX, line.centerY, radius, if (enabled) Theme.text else Theme.textFaint)
	}

	override fun onPointer(event: PointerEvent): Boolean {
		when (event.action) {
			PointerAction.PRESS -> {
				held = true
				updateFrom(event.x)
				return true
			}

			PointerAction.DRAG -> {
				if (!held) return false
				updateFrom(event.x)
				return true
			}

			PointerAction.RELEASE -> {
				if (!held) return false
				held = false
				return true
			}

			PointerAction.WHEEL -> {
				if (!hovered) return false
				value -= event.scroll * step
				return true
			}

			else -> return false
		}
	}

	private fun updateFrom(x: Float) {
		val ratio = ((x - track.x) / track.width).coerceIn(0f, 1f)
		value = min + (max - min) * ratio
	}
}
