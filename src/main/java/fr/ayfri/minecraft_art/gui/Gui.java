package fr.ayfri.minecraft_art.gui;

import fr.ayfri.minecraft_art.Main;
import fr.ayfri.minecraft_art.buttons.Button;
import fr.ayfri.minecraft_art.rendering.DrawZone;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.HashMap;

public class Gui {
	private final Main sketch;
	private final ArrayList<Button> buttons;
	private final HashMap<String, Text> texts;
	private final DrawZone outputZone;
	private final DrawZone inputZone;
	private final DrawZone loadingBar;
	
	public Gui(Main sketch) {
		this.sketch = sketch;
		buttons = new ArrayList<>();
		texts = new HashMap<>();
		inputZone = new DrawZone(sketch, sketch.width / 2 - (512 + 20), 50);
		outputZone = new DrawZone(sketch, sketch.width / 2 + 20, 50);
		loadingBar = new DrawZone(sketch, sketch.width / 2 + 20, 562, 512, 15);
		addText(new Text(sketch, 10, 50, 100, 40), "size");
		addText(new Text(sketch, 10, 80, 150, 40), "width");
		addText(new Text(sketch, 10, 110, 150, 40), "height");
		addText(new Text(sketch, sketch.width / 2 - 315, -22, 150, 80), "Input");
		addText(new Text(sketch, sketch.width / 2 + 235, -22, 150, 80), "Output");
	}
	
	public void addText(Text text, String name) {
		text.setText(name);
		texts.put(name, text);
	}
	
	public void draw() {
		buttons.forEach(Button::draw);
		texts.values().forEach(Text::draw);
		Thread zones = new Thread() {
			@Override
			public void start() {
				inputZone.draw(sketch.inputImage);
				outputZone.draw(sketch.output);
				loadingBar.show();
			}
		};
		
		zones.start();
	}
	
	public Text getText(String name) {
		return texts.get(name);
	}
	
	public void mouseEvent(MouseEvent event) {
		buttons.forEach(button -> button.mouseEvent(event));
	}
	
	public void addButton(Button button) {
		buttons.add(button);
	}
	
	public DrawZone getInputZone() {
		return inputZone;
	}
	
	public DrawZone getLoadingBar() {
		return loadingBar;
	}
	
	public DrawZone getOutputZone() {
		return outputZone;
	}
}
