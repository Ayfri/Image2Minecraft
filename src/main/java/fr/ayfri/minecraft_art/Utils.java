package fr.ayfri.minecraft_art;

import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;

public class Utils {
	
	public static PImage getNearestResizedBlock(PGraphics graphics, int target) {
		float min = Integer.MAX_VALUE;
		PImage value = null;
		float diff;
		
		for (final PImage block : Main.blocks.values()) {
			int color = getAverageColor(graphics, block);
			diff = getColorDistance(graphics, color, target);
			if (diff < min) {
				min = diff;
				value = block;
			}
		}
		
		return value;
	}
	
	public static int getAverageColor(PGraphics graphics, PImage image) {
		PVector average = new PVector();
		image.loadPixels();
		for (final int pixel : image.pixels) {
			average.add(graphics.red(pixel), graphics.green(pixel), graphics.blue(pixel));
		}
		average.div(image.pixels.length);
		
		return graphics.color(average.x, average.y, average.z);
	}
	
	public static float getColorDistance(PGraphics graphics, int a, int b) {
		PVector vector = new PVector(graphics.red(a), graphics.green(a), graphics.blue(a));
		vector.sub(graphics.red(b), graphics.green(b), graphics.blue(b));
		return vector.mag();
	}
	
	public static boolean isInRange(int value, int min, int max) {
		return value > min && value < max;
	}
}