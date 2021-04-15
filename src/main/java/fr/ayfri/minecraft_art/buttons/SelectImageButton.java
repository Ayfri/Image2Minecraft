package fr.ayfri.minecraft_art.buttons;

import fr.ayfri.minecraft_art.Main;

public class SelectImageButton extends Button {
	public SelectImageButton(final Main sketch) {
		super(sketch, sketch.width / 2, sketch.height - 100, 150, 80);
		setText("Select Image");
	}
	
	@Override
	public void onClick() {
		sketch.selectInput("Select an image.", "chooseFile");
	}
}
