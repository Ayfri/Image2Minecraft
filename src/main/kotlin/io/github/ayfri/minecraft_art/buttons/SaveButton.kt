package io.github.ayfri.minecraft_art.buttons

import io.github.ayfri.minecraft_art.Main
import java.io.File

class SaveButton(sketch: Main) : Button(sketch, sketch.width / 2 + 200, sketch.height - 100, 150, 80) {
	init {
		text = "Save"
	}

	override fun onClick() = sketch.selectOutput("Select where to save the image.", "save", File("image.png"))
}
