package io.github.ayfri.minecraft_art.gui

import io.github.ayfri.minecraft_art.Main
import processing.awt.PGraphicsJava2D

class Text(private val sketch: Main, private val x: Int, private val y: Int, width: Int, height: Int) : PGraphicsJava2D() {
	var text = ""
	var size = height / 2f

	init {
		setParent(sketch)
		setPrimary(false)
		setSize(width, height)
	}

	fun draw() {
		beginDraw()
		strokeWeight(4f)
		background(color(255, 255, 255, 0))
		textSize(size)
		textAlign(LEFT, CENTER)
		fill(0)
		text(text, 0f, height / 2f)
		endDraw()

		sketch.image(this, x.toFloat(), y.toFloat())
	}
}
