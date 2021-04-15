package fr.ayfri.minecraft_art.buttons;

import fr.ayfri.minecraft_art.Main;

import java.io.File;

public class SaveButton extends Button {
	public SaveButton(final Main sketch) {
		super(sketch, sketch.width / 2 + 200, sketch.height - 100, 150, 80);
		setText("Save");
	}
	
	@Override
	public void onClick() {
		final File defaultSelectedFile = new File("image.png");
		sketch.selectOutput("Select where to save the image.", "save", defaultSelectedFile);
	}
}
