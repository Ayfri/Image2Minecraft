package io.github.ayfri.minecraft_art.ui.widgets

import io.github.ayfri.minecraft_art.ui.Align
import io.github.ayfri.minecraft_art.ui.Icon
import io.github.ayfri.minecraft_art.ui.KeyEvent
import io.github.ayfri.minecraft_art.ui.PointerAction
import io.github.ayfri.minecraft_art.ui.PointerEvent
import io.github.ayfri.minecraft_art.ui.Rect
import io.github.ayfri.minecraft_art.ui.Renderer
import io.github.ayfri.minecraft_art.ui.Theme
import io.github.ayfri.minecraft_art.ui.Widget
import io.github.ayfri.minecraft_art.ui.icon

/** Select control with a scrollable popup list, typing a letter jumps to the first item starting with it. */
class Dropdown(
	private val placeholder: String,
	items: List<String> = emptyList(),
	private val onSelect: (Int) -> Unit = {},
) : Widget() {
	var items = items
		set(value) {
			field = value
			if (selected >= value.size) selected = -1
		}

	var selected = -1
		set(value) {
			field = value.coerceIn(-1, items.lastIndex)
			scrollToSelection()
		}

	var open = false
		private set

	private var scroll = 0f
	private var hoverIndex = -1

	override val capturing get() = open

	val value get() = items.getOrNull(selected)

	private val rowHeight = 30f
	private val popupHeight get() = minOf(items.size * rowHeight, 270f)
	private val popup get() = Rect(bounds.x, bounds.bottom + 4f, bounds.width, popupHeight)
	private val maxScroll get() = maxOf(0f, items.size * rowHeight - popupHeight)

	fun select(index: Int) {
		selected = index
		if (index >= 0) onSelect(index)
	}

	fun close() {
		open = false
	}

	private fun scrollToSelection() {
		if (selected < 0) return
		scroll = (selected * rowHeight - popupHeight / 2f).coerceIn(0f, maxOf(0f, maxScroll))
	}

	override fun render(renderer: Renderer) {
		val background = Theme.mix(Theme.surfaceRaised, Theme.surfaceHover, hoverAmount)
		renderer.fill(bounds, if (enabled) background else Theme.surface, Theme.RADIUS_SMALL)
		renderer.stroke(bounds, if (open) Theme.accent else Theme.mix(Theme.border, Theme.borderStrong, hoverAmount), Theme.RADIUS_SMALL)

		val label = value ?: placeholder
		val color = if (value == null || !enabled) Theme.textMuted else Theme.text
		renderer.textEllipsis(label, bounds.x + 12f, bounds.centerY, bounds.width - 40f, Theme.body.copy(color = color))
		renderer.icon(Icon.CHEVRON, bounds.right - 16f, bounds.centerY, 14f, Theme.textMuted)
	}

	override fun renderOverlay(renderer: Renderer) {
		if (!open || items.isEmpty()) return

		val area = popup
		renderer.fill(Rect(area.x, area.y + 3f, area.width, area.height), Theme.shadow, Theme.RADIUS)
		renderer.fill(area, Theme.surfaceRaised, Theme.RADIUS)
		renderer.stroke(area, Theme.borderStrong, Theme.RADIUS)

		renderer.pushClip(area.inset(1f))
		items.forEachIndexed { index, item ->
			val y = area.y + index * rowHeight - scroll
			if (y + rowHeight < area.y || y > area.bottom) return@forEachIndexed

			val row = Rect(area.x + 4f, y + 1f, area.width - 8f, rowHeight - 2f)
			when {
				index == selected -> renderer.fill(row, Theme.accentSoft, Theme.RADIUS_SMALL)
				index == hoverIndex -> renderer.fill(row, Theme.surfaceHover, Theme.RADIUS_SMALL)
			}
			val color = if (index == selected) Theme.accentHover else Theme.text
			renderer.textEllipsis(item, row.x + 10f, row.centerY, row.width - 20f, Theme.body.copy(color = color))
		}
		renderer.popClip()

		if (maxScroll > 0f) {
			val barHeight = area.height * (area.height / (items.size * rowHeight))
			val barY = area.y + (area.height - barHeight) * (scroll / maxScroll)
			renderer.fill(Rect(area.right - 6f, barY, 3f, barHeight), Theme.borderStrong, 2f)
		}
	}

	override fun onPointer(event: PointerEvent): Boolean {
		if (!open) {
			if (event.action == PointerAction.RELEASE && hovered) {
				open = true
				scrollToSelection()
				return true
			}
			return event.action == PointerAction.PRESS && hovered
		}

		val area = popup
		val inside = area.contains(event.x, event.y)
		hoverIndex = if (inside) ((event.y - area.y + scroll) / rowHeight).toInt().coerceIn(0, items.lastIndex) else -1

		when (event.action) {
			PointerAction.WHEEL -> if (inside) scroll = (scroll + event.scroll * rowHeight).coerceIn(0f, maxScroll)
			PointerAction.RELEASE -> when {
				inside -> {
					select(hoverIndex)
					open = false
				}

				!bounds.contains(event.x, event.y) -> open = false
			}

			PointerAction.PRESS -> if (!inside && bounds.contains(event.x, event.y)) open = false

			else -> return inside
		}
		return true
	}

	override fun onKey(event: KeyEvent): Boolean {
		if (!open) return false

		when (event.code) {
			27 -> open = false
			38 -> select(maxOf(0, selected - 1))
			40 -> select(minOf(items.lastIndex, selected + 1))
			10 -> open = false
			else -> {
				val target = event.char.lowercaseChar()
				if (!target.isLetterOrDigit()) return false
				val index = items.indexOfFirst { it.firstOrNull()?.lowercaseChar() == target }
				if (index >= 0) select(index)
			}
		}
		return true
	}
}
