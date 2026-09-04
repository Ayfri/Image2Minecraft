package io.github.ayfri.minecraft_art.ui.widgets

import io.github.ayfri.minecraft_art.ui.PointerAction
import io.github.ayfri.minecraft_art.ui.PointerEvent
import io.github.ayfri.minecraft_art.ui.Rect
import io.github.ayfri.minecraft_art.ui.Renderer
import io.github.ayfri.minecraft_art.ui.Theme
import io.github.ayfri.minecraft_art.ui.Widget
import io.github.ayfri.minecraft_art.ui.approach

/** Label on the left, iOS-style switch on the right, the whole row is clickable. */
class Toggle(
	private val label: String,
	checked: Boolean = false,
	private val onChange: (Boolean) -> Unit = {},
) : Widget() {
	var checked = checked
		set(value) {
			if (field == value) return
			field = value
			onChange(value)
		}

	private var knob = if (checked) 1f else 0f

	override fun update(delta: Float) {
		super.update(delta)
		knob = approach(knob, if (checked) 1f else 0f, delta, speed = 18f)
	}

	override fun render(renderer: Renderer) {
		val color = when {
			!enabled -> Theme.textFaint
			hovered -> Theme.text
			else -> Theme.bodyMuted.color
		}
		renderer.textEllipsis(label, bounds.x, bounds.centerY, bounds.width - 46f, Theme.body.copy(color = color))

		val switch = Rect(bounds.right - 36f, bounds.centerY - 10f, 36f, 20f)
		renderer.fill(switch, Theme.mix(Theme.surfaceHover, Theme.accent, knob), 10f)
		renderer.circle(switch.x + 10f + 16f * knob, switch.centerY, 7f, Theme.mix(Theme.textMuted, 0xFFFFFFFF.toInt(), knob))
	}

	override fun onPointer(event: PointerEvent): Boolean {
		when (event.action) {
			PointerAction.PRESS -> {
				held = true
				return true
			}

			PointerAction.RELEASE -> {
				val fired = held && hovered
				held = false
				if (fired) checked = !checked
				return fired
			}

			else -> return false
		}
	}
}
