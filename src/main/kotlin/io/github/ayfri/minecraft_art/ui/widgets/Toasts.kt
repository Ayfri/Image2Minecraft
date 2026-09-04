package io.github.ayfri.minecraft_art.ui.widgets

import io.github.ayfri.minecraft_art.ui.Icon
import io.github.ayfri.minecraft_art.ui.Rect
import io.github.ayfri.minecraft_art.ui.Renderer
import io.github.ayfri.minecraft_art.ui.Theme
import io.github.ayfri.minecraft_art.ui.Widget
import io.github.ayfri.minecraft_art.ui.icon

enum class ToastKind(val color: Int, val glyph: Icon) {
	INFO(Theme.accent, Icon.CHECK),
	SUCCESS(Theme.success, Icon.CHECK),
	WARNING(Theme.warning, Icon.LIST),
	ERROR(Theme.danger, Icon.TRASH),
}

/** Stack of transient notifications drawn in the bottom-right corner, entries fade out on their own. */
class Toasts : Widget() {
	private data class Entry(val message: String, val kind: ToastKind, var life: Float)

	private val entries = ArrayDeque<Entry>()

	fun show(message: String, kind: ToastKind = ToastKind.INFO, seconds: Float = 3.5f) {
		synchronized(entries) {
			entries.addFirst(Entry(message, kind, seconds))
			while (entries.size > 4) entries.removeLast()
		}
	}

	override fun update(delta: Float) {
		synchronized(entries) {
			entries.forEach { it.life -= delta }
			entries.removeAll { it.life <= 0f }
		}
	}

	override fun render(renderer: Renderer) {
		val snapshot = synchronized(entries) { entries.toList() }
		var y = bounds.bottom

		snapshot.forEach { entry ->
			val alpha = entry.life.coerceIn(0f, 0.35f) / 0.35f
			val width = minOf(bounds.width, renderer.textWidth(entry.message, Theme.body) + 62f)
			val area = Rect(bounds.right - width, y - 42f, width, 36f)

			renderer.fill(Rect(area.x, area.y + 3f, area.width, area.height), Theme.withAlpha(Theme.shadow, 0.3f * alpha), Theme.RADIUS)
			renderer.fill(area, Theme.withAlpha(Theme.surfaceRaised, alpha), Theme.RADIUS)
			renderer.stroke(area, Theme.withAlpha(entry.kind.color, 0.55f * alpha), Theme.RADIUS)
			renderer.icon(entry.kind.glyph, area.x + 22f, area.centerY, 14f, Theme.withAlpha(entry.kind.color, alpha))
			renderer.textEllipsis(entry.message, area.x + 38f, area.centerY, area.width - 50f, Theme.body.copy(color = Theme.withAlpha(Theme.text, alpha)))
			y = area.y
		}
	}
}
