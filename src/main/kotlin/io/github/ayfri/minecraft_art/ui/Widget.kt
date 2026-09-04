package io.github.ayfri.minecraft_art.ui

/** Base of every UI element: a rectangle, animated hover and press states, and normalised event handlers. */
abstract class Widget {
	val bounds = Rect()
	var visible = true
	var enabled = true
	var tooltip: String? = null

	var hovered = false
		internal set
	var held = false
		protected set

	protected var hoverAmount = 0f
	protected var pressAmount = 0f

	/** While true the widget receives every pointer event, used by open dropdowns and in-progress drags. */
	open val capturing get() = false

	fun place(x: Float, y: Float, width: Float, height: Float) = apply { bounds.set(x, y, width, height) }

	open fun update(delta: Float) {
		hoverAmount = approach(hoverAmount, if (hovered && enabled) 1f else 0f, delta)
		pressAmount = approach(pressAmount, if (held && enabled) 1f else 0f, delta, speed = 26f)
	}

	abstract fun render(renderer: Renderer)

	/** Second pass drawn above every widget, for popups that must escape their own bounds. */
	open fun renderOverlay(renderer: Renderer) = Unit

	/** Returns true when the event is consumed and must not reach widgets below. */
	open fun onPointer(event: PointerEvent) = false

	open fun onKey(event: KeyEvent) = false
}

/** Owns the widget tree, routes pointer and key events, and drives the two render passes. */
class UiRoot {
	private val widgets = mutableListOf<Widget>()
	private var active: Widget? = null

	var pointerX = 0f
		private set
	var pointerY = 0f
		private set

	fun add(vararg items: Widget) = items.forEach(widgets::add)

	fun remove(widget: Widget) = widgets.remove(widget)

	/** Widget currently under the pointer, used by the tooltip layer. */
	val hovered get() = widgets.lastOrNull { it.visible && it.enabled && it.hovered }

	fun onPointer(event: PointerEvent) {
		if (event.action != PointerAction.EXIT) {
			pointerX = event.x
			pointerY = event.y
		}

		val target = widgets.filter { it.visible && it.enabled }
		target.forEach { it.hovered = it.bounds.contains(event.x, event.y) }

		val capturing = target.lastOrNull(Widget::capturing)
		if (capturing != null && capturing.onPointer(event)) return

		when (event.action) {
			PointerAction.PRESS -> active = target.lastOrNull { it.hovered && it.onPointer(event) }
			PointerAction.DRAG, PointerAction.RELEASE -> {
				val handled = active?.onPointer(event) ?: false
				if (event.action == PointerAction.RELEASE) active = null
				if (!handled) target.lastOrNull { it.hovered }?.onPointer(event)
			}

			else -> target.lastOrNull { it.hovered }?.onPointer(event)
		}
	}

	fun onKey(event: KeyEvent) = widgets.any { it.visible && it.enabled && it.onKey(event) }

	fun render(renderer: Renderer) {
		val visible = widgets.filter(Widget::visible)
		visible.forEach { it.update(renderer.delta) }
		visible.forEach { it.render(renderer) }
		visible.forEach { it.renderOverlay(renderer) }
	}
}
