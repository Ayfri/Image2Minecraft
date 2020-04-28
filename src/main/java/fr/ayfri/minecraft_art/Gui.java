package fr.ayfri.minecraft_art;

import fr.ayfri.minecraft_art.buttons.Button;
import processing.event.MouseEvent;

import java.util.ArrayList;

public class Gui {
	private final Main sketch;
	private final ArrayList<Button> buttons;
	private final DrawZone outputZone;
	private final DrawZone inputZone;
	private final DrawZone loadingBar;
	
	public Gui(Main sketch) {
		this.sketch = sketch;
		buttons = new ArrayList<>();
		inputZone = new DrawZone(sketch, sketch.width / 2 - (512 + 20), 20);
		outputZone = new DrawZone(sketch, sketch.width / 2 + 20, 20);
		loadingBar = new DrawZone(sketch, sketch.width / 2 + 20, 532, 512, 15);
	}
	
	public void draw() {
		buttons.forEach(Button::draw);
		inputZone.draw(sketch.inputImage);
		outputZone.draw(sketch.output);
		loadingBar.show();
	}
	
	public void mouseEvent(MouseEvent event) {
		buttons.forEach(button -> button.mouseEvent(event));
	}
	
	public void addButton(Button button) {
		buttons.add(button);
	}
	
	public DrawZone getLoadingBar() {
		return loadingBar;
	}
}
