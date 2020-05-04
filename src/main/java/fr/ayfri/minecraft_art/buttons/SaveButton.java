package fr.ayfri.minecraft_art.buttons;

import fr.ayfri.minecraft_art.Main;

public class SaveButton extends Button {
	public SaveButton(final Main sketch) {
		super(sketch, sketch.width / 2 + 200, sketch.height - 100, 150, 80);
		setText("Save");
	}
	
	@Override
	public void onClick() {
		sketch.save();
	}
}
