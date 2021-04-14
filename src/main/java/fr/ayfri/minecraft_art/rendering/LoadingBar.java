package fr.ayfri.minecraft_art.rendering;

import fr.ayfri.minecraft_art.Main;

public class LoadingBar extends DrawZone {
	public int percentage = 0;
	
	public LoadingBar(final Main sketch, final int x, final int y, final int width, final int height) {
		super(sketch, x, y, width, height);
	}
	
	public void draw() {
		final float rectSize = width * (percentage / 100f);
		beginDraw();
		noStroke();
		fill(100, 200, 255);
		rect(0, 0, rectSize, height);
		fill(0);
		textSize(12);
		textAlign(CENTER, CENTER);
		text(percentage + "%", rectSize / 2f, height / 2f);
		endDraw();
		show();
	}
	
	public void setPercentage(final int percentage) {
		this.percentage = percentage;
	}
}
