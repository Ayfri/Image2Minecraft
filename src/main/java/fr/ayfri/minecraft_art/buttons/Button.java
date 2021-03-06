package fr.ayfri.minecraft_art.buttons;

import fr.ayfri.minecraft_art.Main;
import fr.ayfri.minecraft_art.Utils;
import processing.awt.PGraphicsJava2D;
import processing.event.MouseEvent;

public abstract class Button extends PGraphicsJava2D {
	private final int color;
	private final int clickedColor;
	private final int x;
	private final int width;
	private final int height;
	private final int y;
	private String text;
	private boolean clicked = false;
	protected final Main sketch;
	
	public Button(final Main sketch, final int x, final int y, final int width, final int height) {
		this.width = width;
		this.height = height;
		this.sketch = sketch;
		this.x = x - width / 2;
		this.y = y;
		text = "";
		color = sketch.color(200f);
		clickedColor = sketch.color(100f);
		setParent(sketch);
		setPrimary(false);
		setSize(width, height);
	}
	
	public void draw() {
		hint(DISABLE_DEPTH_TEST);
		beginDraw();
		background(clicked ? clickedColor : color);
		textSize(height / 4f);
		textAlign(CENTER, CENTER);
		fill(0);
		text(text,
		     width / 2f,
		     height / 2f);
		endDraw();
		hint(ENABLE_DEPTH_TEST);
		
		if (sketch.mouseButton == 0) {
			clicked = false;
		}
		
		sketch.image(this, x, y);
	}
	
	public void mouseEvent(final MouseEvent event) {
		if (Utils.isInRange(event.getX(), x, x + width) && Utils.isInRange(event.getY(), y, y + height)) {
			switch (event.getAction()) {
				case MouseEvent.PRESS -> {
					clicked = true;
					onClick();
				}
				case MouseEvent.RELEASE -> clicked = false;
			}
		}
	}
	
	public abstract void onClick();
	
	public void setClicked(final boolean clicked) {
		this.clicked = clicked;
	}
	
	public void setText(final String text) {
		this.text = text;
	}
}
