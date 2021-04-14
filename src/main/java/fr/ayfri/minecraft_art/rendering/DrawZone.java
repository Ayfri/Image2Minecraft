package fr.ayfri.minecraft_art.rendering;

import fr.ayfri.minecraft_art.Main;
import processing.awt.PGraphicsJava2D;
import processing.core.PImage;

public class DrawZone extends PGraphicsJava2D {
	protected final Main sketch;
	protected final int x;
	protected final int y;
	
	public DrawZone(final Main sketch, final int x, final int y) {
		this(sketch, x, y, 512, 512);
	}
	
	public DrawZone(final Main sketch, final int x, final int y, final int width, final int height) {
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
	
	public void draw(final PImage renderer) {
		noSmooth();
		beginDraw();
		strokeWeight(4);
		background(0);
		image(renderer, 0, 0, width, height);
		noStroke();
		endDraw();
		
		sketch.image(this, x, y);
	}
	
	public void show() {
		sketch.image(this, x, y);
	}
}
