package fr.ayfri.minecraft_art.gui;

import fr.ayfri.minecraft_art.Main;
import processing.awt.PGraphicsJava2D;

public class Text extends PGraphicsJava2D {
	private final Main sketch;
	private final int x;
	private final int y;
	private String text;
	
	public Text(Main sketch, int x, int y, int width, int height) {
		this.width = width;
		this.height = height;
		this.sketch = sketch;
		this.x = x;
		this.y = y;
		text = "";
		setParent(sketch);
		setPrimary(false);
		setSize(width, height);
	}
	
	public void draw() {
		beginDraw();
		strokeWeight(4);
		background(color(255, 255, 255, 0));
		textSize(height / 2f);
		textAlign(LEFT, CENTER);
		fill(0);
		text(text, 0, height / 2f);
		endDraw();
		
		sketch.image(this, x, y);
	}
	
	public String getText() {
		return text;
	}
	
	public void setText(final String text) {
		this.text = text;
	}
}
