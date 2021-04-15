package fr.ayfri.minecraft_art;

import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;

import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

public class Utils {
	public static PImage getNearestResizedBlock(final PGraphics graphics, final int target) {
		float min = Integer.MAX_VALUE;
		PImage value = null;
		float diff;
		for (final Map.Entry<String, PImage> blockEntry : Main.BLOCKS.entrySet()) {
			final int color = Main.COLORS_OF_BLOCKS.get(blockEntry.getKey());
			diff = getColorDistance(graphics, color, target);
			if (diff < min) {
				min = diff;
				value = blockEntry.getValue();
			}
		}

//		for (final PImage block : Main.blocks.values()) {
//
//		}
		final PImage finalValue = value;
		final Optional<Entry<String, PImage>> first = Main.BLOCKS.entrySet().stream().filter(blockName -> Objects.equals(blockName.getValue(), finalValue)).findFirst();
		assert first.isPresent();
		final String block = first.get().getKey();
		if (Main.blocksUsed.containsKey(block)) {
			final int number = Main.blocksUsed.get(block) + 1;
			Main.blocksUsed.remove(block);
			Main.blocksUsed.put(block, number);
		} else {
			Main.blocksUsed.put(block, 1);
		}
		return value;
	}
	
	public static float getColorDistance(final PGraphics graphics, final int a, final int b) {
		final PVector vector = new PVector(graphics.red(a), graphics.green(a), graphics.blue(a));
		vector.sub(graphics.red(b), graphics.green(b), graphics.blue(b));
		return vector.mag();
	}
	
	public static int getAverageColor(final PImage image) {
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
	
	public static boolean isInRange(final int value, final int min, final int max) {
		return value > min && value < max;
	}
	
	public static boolean listNotContainsItem(final String[] items, final String inputStr) {
		return Arrays.stream(items).parallel().noneMatch(inputStr::contains);
	}
}
