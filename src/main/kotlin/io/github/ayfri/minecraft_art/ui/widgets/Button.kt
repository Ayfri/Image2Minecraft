package io.github.ayfri.minecraft_art.ui.widgets

import io.github.ayfri.minecraft_art.ui.Align
import io.github.ayfri.minecraft_art.ui.Icon
import io.github.ayfri.minecraft_art.ui.PointerAction
import io.github.ayfri.minecraft_art.ui.PointerEvent
import io.github.ayfri.minecraft_art.ui.Rect
import io.github.ayfri.minecraft_art.ui.Renderer
import io.github.ayfri.minecraft_art.ui.Theme
import io.github.ayfri.minecraft_art.ui.Widget
import io.github.ayfri.minecraft_art.ui.icon

enum class ButtonStyle { PRIMARY, SECONDARY, GHOST }

class Button(
	var label: String,
	private val style: ButtonStyle = ButtonStyle.SECONDARY,
	private val icon: Icon? = null,
	private val onClick: () -> Unit,
) : Widget() {
	/** Compact buttons only show their icon, the label is then reused as the tooltip. */
	var iconOnly = false

	override fun render(renderer: Renderer) {
		val base = when (style) {
			ButtonStyle.PRIMARY -> Theme.accent
			ButtonStyle.SECONDARY -> Theme.surfaceRaised
			ButtonStyle.GHOST -> Theme.withAlpha(Theme.surfaceRaised, 0f)
		}
		val top = when (style) {
			ButtonStyle.PRIMARY -> Theme.accentHover
			else -> Theme.surfaceHover
		}
		val background = Theme.mix(base, top, hoverAmount * 0.9f - pressAmount * 0.4f)
		val content = when {
			!enabled -> Theme.textFaint
			style == ButtonStyle.PRIMARY -> 0xFF0B1220.toInt()
			else -> Theme.mix(Theme.textMuted, Theme.text, hoverAmount)
		}

		renderer.fill(bounds, if (enabled) background else Theme.surface, Theme.RADIUS_SMALL)
		if (style != ButtonStyle.PRIMARY) {
			renderer.stroke(bounds, Theme.mix(Theme.border, Theme.borderStrong, hoverAmount), Theme.RADIUS_SMALL)
		}

		val glyphSize = minOf(16f, bounds.height * 0.44f)
		if (iconOnly || label.isEmpty()) {
			icon?.let { renderer.icon(it, bounds.centerX, bounds.centerY, glyphSize, content) }
			return
		}

		val textStyle = Theme.body.copy(color = content, bold = style == ButtonStyle.PRIMARY, align = Align.LEFT)
		val labelWidth = renderer.textWidth(label, textStyle)
		val iconWidth = if (icon == null) 0f else glyphSize + 8f
		val start = bounds.centerX - (labelWidth + iconWidth) / 2f
		icon?.let { renderer.icon(it, start + glyphSize / 2f, bounds.centerY, glyphSize, content) }
		renderer.text(label, start + iconWidth, bounds.centerY, textStyle)
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
				if (fired) onClick()
				return fired
			}

			else -> return false
		}
	}
}

/** Row of buttons sharing one rectangle, laid out with equal widths. */
fun layoutRow(rect: Rect, gap: Float, widgets: List<Widget>) {
	val visible = widgets.filter { it.visible }
	if (visible.isEmpty()) return

	val width = (rect.width - gap * (visible.size - 1)) / visible.size
	visible.forEachIndexed { index, widget -> widget.place(rect.x + (width + gap) * index, rect.y, width, rect.height) }
}
