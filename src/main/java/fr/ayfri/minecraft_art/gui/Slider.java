package fr.ayfri.minecraft_art.gui;

import fr.ayfri.minecraft_art.Main;
import processing.core.PApplet;
import processing.core.PConstants;

public class Slider {
	private final Main sketch;
	private final int x;
	private final int width;
	private final int height;
	private final float y;
	private float min = 1;
	private float max = 100;
	private int value;
	private float trueValue;
	public boolean isOn = false;
	
	public Slider(final Main sketch, final int x, final int y, final int width, final int height) {
		this.sketch = sketch;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.value = width / 2;
		this.trueValue = max / 2;
	}
	
	public void draw() {
		sketch.noStroke();
		sketch.fill(240);
		sketch.rect(x, y, width, height / 2f);
		sketch.fill(255);
		sketch.rect(x + value, y - height / 2f, 10, 3 * height / 2f);
		sketch.fill(0);
		sketch.textAlign(PConstants.CENTER, PConstants.CENTER);
		sketch.textSize(17f);
		sketch.text(String.valueOf(trueValue / 10f).substring(0, 3), x + width / 2f, y - 26);
	}
	
	public void update(final int value) {
		isOn = true;
		if (value - x < 0 || value - x > width) {
			return;
		}
		
		trueValue = PApplet.map(value - x, 0, width, min, max);
		if (trueValue < min) {
			trueValue = min;
		}
		if (trueValue > max) {
			trueValue = max;
		}
		
		this.value = value - x;
	}
	
	public float getValue() {
		return trueValue / 10f;
	}
	
	public boolean isOver() {
		return (x + width >= sketch.mouseX) && (sketch.mouseX >= x) && (y + height >= sketch.mouseY) && (sketch.mouseY >= y);
	}
	
	public void setMax(final float max) {
		this.max = max;
		this.trueValue = max / 2;
	}
	
	public void setMin(final float min) {
		this.min = min;
	}
}
