package fr.ayfri.minecraft_art;

import processing.core.PApplet;
import processing.core.PImage;
import processing.core.PVector;

public class Utils {
	
	public static PImage getNearestResizedBlock(Main sketch, int target) {
		float min = Integer.MAX_VALUE;
		PImage value = null;
		float diff;
		
		for (final PImage block : Main.blocks.values()) {
			int color = getAverageColor(sketch, block);
			diff = getColorDistance(sketch, color, target);
			if (diff < min) {
				min = diff;
				value = block;
			}
		}
		
		return value;
	}
	
	public static float getColorDistance(PApplet sketch, int a, int b) {
		PVector vector = new PVector(sketch.red(a), sketch.green(a), sketch.blue(a));
		vector.sub(sketch.red(b), sketch.green(b), sketch.blue(b));
		return vector.mag();
	}
	
	public static int getAverageColor(PApplet sketch, PImage image) {
		PVector average = new PVector();
		image.loadPixels();
		for (final int pixel : image.pixels) {
			average.add(sketch.red(pixel), sketch.green(pixel), sketch.blue(pixel));
		}
		average.div(image.pixels.length);
		
		return sketch.color(average.x, average.y, average.z);
	}
	
	public static boolean isInRange(int value, int min, int max) {
		return value > min && value < max;
	}
}