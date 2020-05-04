package fr.ayfri.minecraft_art.rendering;

import fr.ayfri.minecraft_art.Main;
import processing.awt.PGraphicsJava2D;
import processing.core.PImage;

public class DrawZone extends PGraphicsJava2D {
	protected final Main sketch;
	protected final int x;
	protected final int y;
	
	public DrawZone(Main sketch, int x, int y) {
		this(sketch, x, y, 512, 512);
	}
	
	public DrawZone(Main sketch, int x, int y, int width, int height) {
		this.sketch = sketch;
		this.width = width;
		this.height = height;
		this.x = x;
		this.y = y;
		setParent(sketch);
		setPrimary(false);
		setSize(width, height);
//		setPath(sketch.savePath(""));
	}
	
	public void draw(PImage renderer) {
		beginDraw();
		strokeWeight(4);
		background(0);
		image(renderer, 0, 0, width, height);
		noFill();
		endDraw();
		
		sketch.image(this, x, y);
	}
	
	public void show() {
		sketch.image(this, x, y);
	}
}
