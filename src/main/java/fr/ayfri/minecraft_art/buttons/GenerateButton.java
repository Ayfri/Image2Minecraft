package fr.ayfri.minecraft_art.buttons;

import fr.ayfri.minecraft_art.Main;
import fr.ayfri.minecraft_art.ResourceUtils;

public class GenerateButton extends Button {
	
	public GenerateButton(final Main sketch) {
		super(sketch, sketch.width / 2 - 200, sketch.height - 100, 150, 80);
		setText("Generate");
		System.out.println("Loading blocks");
	}
	
	@Override
	public void onClick() {
//		sketch.thread("justPutInputInOutput");
		sketch.thread("generate");
		
		ResourceUtils.registerBlocksFromVersion("21w15a");
	}
}
