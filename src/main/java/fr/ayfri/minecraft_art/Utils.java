package fr.ayfri.minecraft_art;

import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;

import java.util.Objects;

public class Utils {
	
	public static PImage getNearestResizedBlock(PGraphics graphics, int target) {
		float min = Integer.MAX_VALUE;
		PImage value = null;
		float diff;
		for (final PImage block : Main.blocks.values()) {
			int color = getAverageColor(block);
			diff = getColorDistance(graphics, color, target);
			if (diff < min) {
				min = diff;
				value = block;
			}
		}
		final PImage finalValue = value;
		String block = Main.blocks.entrySet().stream().filter(blockName -> Objects.equals(blockName.getValue(), finalValue)).findFirst().get().getKey();
		if (Main.blocksUsed.containsKey(block)) {
			int number = Main.blocksUsed.get(block) + 1;
			Main.blocksUsed.remove(block);
			Main.blocksUsed.put(block, number);
		} else {
			Main.blocksUsed.put(block, 1);
		}
		return value;
	}
	
	public static int getAverageColor(PImage image) {
		int red = 0;
		int green = 0;
		int blue = 0;
		
		image.loadPixels();
		final int[] pixels = image.pixels;
		
		for (final int pixel : pixels) {
			red += pixel >> 16 & 0xff;
			green += pixel >> 8 & 0xff;
			blue += pixel & 0xff;
		}
		red /= pixels.length;
		green /= pixels.length;
		blue /= pixels.length;
		
		return red << 16 | green << 8 | blue;
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