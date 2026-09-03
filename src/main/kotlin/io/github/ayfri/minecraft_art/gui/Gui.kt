package io.github.ayfri.minecraft_art.gui

import io.github.ayfri.minecraft_art.Main
import io.github.ayfri.minecraft_art.buttons.Button
import io.github.ayfri.minecraft_art.buttons.GenerateButton
import io.github.ayfri.minecraft_art.buttons.SaveButton
import io.github.ayfri.minecraft_art.buttons.SelectImageButton
import io.github.ayfri.minecraft_art.rendering.DrawZone
import io.github.ayfri.minecraft_art.rendering.LoadingBar
import processing.event.MouseEvent

class Gui(private val sketch: Main) {
	val inputZone = DrawZone(sketch, sketch.width / 2 - (512 + 20), 50)
	val outputZone = DrawZone(sketch, sketch.width / 2 + 20, 50)
	val loadingBar = LoadingBar(sketch, sketch.width / 2 + 20, 562, 512, 15)

	val generateButton = GenerateButton(sketch)
	val saveButton = SaveButton(sketch)
	val selectImageButton = SelectImageButton(sketch)
	val buttons: List<Button> = listOf(generateButton, saveButton, selectImageButton)

	val sliders = mutableListOf<Slider>()
	val texts = mutableMapOf<String, Text>()

	init {
		sliders += Slider(sketch, sketch.width - 400, sketch.height - 50, 250, 10)

		addText(Text(sketch, 10, 50, 150, 40), "size")
		addText(Text(sketch, 10, 80, 150, 40), "width")
		addText(Text(sketch, 10, 110, 150, 40), "height")
		addText(Text(sketch, 10, 150, 250, 60), "pixels")
		texts.getValue("pixels").size = 20f

		addText(Text(sketch, sketch.width - 165, 80, 150, 32), "out-width")
		addText(Text(sketch, sketch.width - 165, 100, 150, 32), "out-height")

		addText(Text(sketch, sketch.width / 2 - 315, -22, 150, 80), "Input")
		addText(Text(sketch, sketch.width / 2 + 235, -22, 150, 80), "Output")
	}

	fun addText(text: Text, name: String) {
		text.text = name
		texts[name] = text
	}

	fun draw() {
		buttons.forEach(Button::draw)
		sliders.forEach(Slider::draw)
		texts.values.forEach(Text::draw)

		sketch.inputImage?.let(inputZone::draw) ?: inputZone.clearZone()
		outputZone.draw(sketch.output)
		loadingBar.show()
	}

	fun mouseEvent(event: MouseEvent) {
		buttons.forEach { it.mouseEvent(event) }

		when (event.action) {
			MouseEvent.DRAG -> sliders.forEach { if (it.isOn || it.isOver) it.update(event.x) }
			MouseEvent.RELEASE -> sliders.forEach { it.isOn = false }
		}
	}
}
