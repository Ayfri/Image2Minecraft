package io.github.ayfri.minecraft_art.ui.widgets

import io.github.ayfri.minecraft_art.ui.Align
import io.github.ayfri.minecraft_art.ui.PointerAction
import io.github.ayfri.minecraft_art.ui.PointerEvent
import io.github.ayfri.minecraft_art.ui.Rect
import io.github.ayfri.minecraft_art.ui.Renderer
import io.github.ayfri.minecraft_art.ui.Theme
import io.github.ayfri.minecraft_art.ui.Widget

data class UsageEntry(val name: String, val color: Int, val count: Int, val share: Float)

/** Scrollable ranking of the blocks used by the last generation, each row shows a swatch, a share bar and a count. */
class UsageList(private val emptyHint: String) : Widget() {
	var entries: List<UsageEntry> = emptyList()
		set(value) {
			field = value
			scroll = 0f
		}

	private var scroll = 0f
	private val rowHeight = 26f
	private val maxScroll get() = maxOf(0f, entries.size * rowHeight - bounds.height)

	override fun render(renderer: Renderer) {
		if (entries.isEmpty()) {
			renderer.text(emptyHint, bounds.x, bounds.y + 14f, Theme.small.copy(color = Theme.textFaint))
			return
		}

		renderer.pushClip(bounds)
		entries.forEachIndexed { index, entry ->
			val y = bounds.y + index * rowHeight - scroll
			if (y + rowHeight < bounds.y || y > bounds.bottom) return@forEachIndexed

			val row = Rect(bounds.x, y, bounds.width, rowHeight)
			renderer.fill(Rect(row.x, row.centerY - 7f, 14f, 14f), entry.color, 3f)
			renderer.fill(Rect(row.x + 22f, row.bottom - 5f, (row.width - 22f) * entry.share, 2f), Theme.withAlpha(Theme.accent, 0.55f), 1f)
			renderer.textEllipsis(entry.name, row.x + 22f, row.centerY - 2f, row.width - 90f, Theme.small.copy(color = Theme.text))
			renderer.text(formatCount(entry.count), row.right, row.centerY - 2f, Theme.small.copy(align = Align.RIGHT))
		}
		renderer.popClip()
	}

	override fun onPointer(event: PointerEvent): Boolean {
		if (event.action != PointerAction.WHEEL || !hovered || maxScroll <= 0f) return false
		scroll = (scroll + event.scroll * rowHeight).coerceIn(0f, maxScroll)
		return true
	}

	private fun formatCount(count: Int) = when {
		count >= 1_000_000 -> "%.1fM".format(count / 1_000_000f)
		count >= 10_000 -> "${count / 1000}k"
		else -> count.toString()
	}
}
