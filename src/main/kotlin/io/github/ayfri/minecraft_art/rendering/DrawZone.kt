package io.github.ayfri.minecraft_art.rendering

import io.github.ayfri.minecraft_art.Main
import processing.awt.PGraphicsJava2D
import processing.core.PImage

open class DrawZone(
	protected val sketch: Main,
	protected val x: Int,
	protected val y: Int,
	width: Int = 512,
	height: Int = 512,
) : PGraphicsJava2D() {
	init {
		setParent(sketch)
		setPrimary(false)
		setSize(width, height)
		/** Blocks are 16x16 pixel art, they must stay crisp. Processing only accepts this before the first beginDraw(). */
		noSmooth()
	}

	fun draw(renderer: PImage) {
		beginDraw()
		strokeWeight(4f)
		background(0)
		image(renderer, 0f, 0f, width.toFloat(), height.toFloat())
		noStroke()
		endDraw()

		show()
	}

	/** Renders the empty placeholder used while no image is loaded yet. */
	fun clearZone() {
		beginDraw()
		background(0)
		endDraw()

		show()
	}

	fun show() = sketch.image(this, x.toFloat(), y.toFloat())
}
