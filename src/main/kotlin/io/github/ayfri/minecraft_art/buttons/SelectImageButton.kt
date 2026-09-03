package io.github.ayfri.minecraft_art.buttons

import io.github.ayfri.minecraft_art.Main

class SelectImageButton(sketch: Main) : Button(sketch, sketch.width / 2, sketch.height - 100, 150, 80) {
	init {
		text = "Select Image"
	}

	override fun onClick() = sketch.selectInput("Select an image.", "chooseFile")
}
