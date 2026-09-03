package io.github.ayfri.minecraft_art.gui

import io.github.ayfri.minecraft_art.Main
import processing.core.PApplet
import processing.core.PConstants

data class Slider(val sketch: Main, val x: Int, val y: Int, val width: Int, val height: Int) {
	private var handleOffset = width / 2
	private var trueValue = 50f

	var min = 1f
	var max = 100f
		set(value) {
			field = value
			trueValue = value / 2
		}

	/** Ratio applied to the input image, [trueValue] stays on a 0..[max] scale and is only scaled down when read. */
	val value get() = trueValue / 10f
	val isOver get() = sketch.mouseX in x..(x + width) && sketch.mouseY in y..(y + height)
	var isOn = false

	fun draw() = with(sketch) {
		noStroke()
		fill(240)
		rect(x.toFloat(), y.toFloat(), width.toFloat(), height / 2f)
		fill(255)
		rect((x + handleOffset).toFloat(), y - height / 2f, 10f, 3 * height / 2f)
		fill(0)
		textAlign(PConstants.CENTER, PConstants.CENTER)
		textSize(17f)
		text(value.toString().take(3), x + width / 2f, y - 26f)
	}

	fun update(mouseX: Int) {
		isOn = true
		val offset = mouseX - x
		if (offset < 0 || offset > width) return

		trueValue = PApplet.map(offset.toFloat(), 0f, width.toFloat(), min, max).coerceIn(min, max)
		handleOffset = offset
	}
}
