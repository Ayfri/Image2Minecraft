package io.github.ayfri.minecraft_art.buttons

import io.github.ayfri.minecraft_art.Main
import io.github.ayfri.minecraft_art.isInRange
import processing.awt.PGraphicsJava2D
import processing.event.MouseEvent

abstract class Button(protected val sketch: Main, x: Int, private val y: Int, width: Int, height: Int) : PGraphicsJava2D() {
	private val x = x - width / 2
	private val color = sketch.color(200f)
	private val clickedColor = sketch.color(100f)
	private var clicked = false
	var text = ""

	init {
		setParent(sketch)
		setPrimary(false)
		setSize(width, height)
	}

	open fun draw() {
		hint(DISABLE_DEPTH_TEST)
		beginDraw()
		background(if (clicked) clickedColor else color)
		textSize(height / 4f)
		textAlign(CENTER, CENTER)
		fill(0)
		text(text, width / 2f, height / 2f)
		endDraw()
		hint(ENABLE_DEPTH_TEST)

		if (sketch.mouseButton == 0) clicked = false
		sketch.image(this, x.toFloat(), y.toFloat())
	}

	open fun mouseEvent(event: MouseEvent) {
		if (!isInRange(event.x, x, x + width) || !isInRange(event.y, y, y + height)) return

		when (event.action) {
			MouseEvent.PRESS -> {
				clicked = true
				onClick()
			}

			MouseEvent.RELEASE -> clicked = false
		}
	}

	abstract fun onClick()
}
