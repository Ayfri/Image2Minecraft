package io.github.ayfri.minecraft_art.rendering

import io.github.ayfri.minecraft_art.Main

class LoadingBar(sketch: Main, x: Int, y: Int, width: Int, height: Int) : DrawZone(sketch, x, y, width, height) {
	var percentage = 0

	fun draw() {
		val rectSize = width * (percentage / 100f)
		beginDraw()
		noStroke()
		fill(100f, 200f, 255f)
		rect(0f, 0f, rectSize, height.toFloat())
		fill(0)
		textSize(12f)
		textAlign(CENTER, CENTER)
		text("$percentage%", rectSize / 2f, height / 2f)
		endDraw()

		show()
	}
}
