package fr.ayfri.minecraft_art;

import processing.awt.PGraphicsJava2D;
import processing.core.PImage;

public class DrawZone extends PGraphicsJava2D {
	private final Main sketch;
	private final int x;
	private final int y;
	
	public DrawZone(Main sketch, int x, int y) {
		this(sketch, x, y, 512,512);
	}
	
	public DrawZone(Main sketch, int x, int y, int width, int length) {
		this.sketch = sketch;
		this.width = width;
		this.height = length;
		this.x = x;
		this.y = y;
		setParent(sketch);
		setPrimary(false);
		setSize(width, height);
	}
	
	public void draw(PImage renderer) {
		beginDraw();
		strokeWeight(4);
		background(0);
		image(renderer, 0, 0, width, height);
		endDraw();
		
		sketch.image(this, x, y);
	}
	
	public void show() {
		sketch.image(this, x, y);
	}
}
