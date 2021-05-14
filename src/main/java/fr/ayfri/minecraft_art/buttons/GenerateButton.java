package fr.ayfri.minecraft_art.buttons;

import fr.ayfri.minecraft_art.Main;
import fr.ayfri.minecraft_art.ResourceUtils;
import fr.ayfri.minecraft_art.Utils;
import fr.ayfri.minecraft_art.rendering.LoadingBar;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;

import java.util.HashMap;
import java.util.TreeMap;

public class GenerateButton extends Button {
	
	public GenerateButton(final Main sketch) {
		super(sketch, sketch.width / 2 - 200, sketch.height - 100, 150, 80);
		setText("Generate");
		System.out.println("Loading blocks");
	}
	
	@Override
	public void onClick() {
		sketch.threadPool.submit(this::generate);
		ResourceUtils.registerBlocksFromVersion("21w15a");
	}
	
	public void generate() {
		System.out.println((char) 27 + "[31mStarting Generate process. ");
		final float ratio = sketch.gui.getSliders().get(0).getValue();
		final int blockSize = 16;
		final long start = System.currentTimeMillis();
		final LoadingBar loading = sketch.gui.getLoadingBar();
		final PImage resizedImage = sketch.inputImage.copy();
		int x = -blockSize;
		int y = 0;
		
		Main.blocksUsed = new HashMap<>();
		resizedImage.resize((int) (sketch.inputImage.width * ratio), (int) (sketch.inputImage.height * ratio));
		resizedImage.loadPixels();
		sketch.output = sketch.createGraphics(resizedImage.width * blockSize, resizedImage.height * blockSize);
		System.out.println("Number of pixels : " + resizedImage.pixels.length);
		
		sketch.output.beginDraw();
		for (int i = 0; i < resizedImage.pixels.length; i++) {
			x += blockSize;
			if (x >= resizedImage.width * blockSize) {
				x = 0;
				y += blockSize;
			}
			final PImage color = Utils.getNearestResizedBlock(sketch.gui.getOutputZone(), resizedImage.pixels[i]);
			sketch.output.image(color, x, y, blockSize, blockSize);
			
			loading.setPercentage((int) PApplet.map(i, 0, resizedImage.pixels.length, 0, 100));
			loading.draw();
		}
		sketch.output.endDraw();
		loading.beginDraw();
		loading.clear();
		loading.endDraw();
		System.out.println(System.currentTimeMillis() - start + "ms time took.");
		System.out.println("Image proceeded");
		new TreeMap<>(Main.blocksUsed).forEach((key, value) -> System.out.println(key.substring(0, key.length() - 4) + " : " + value));
	}
	
	
	/**
	 * This is for testing other way of rendering.
	 */
	public void justPutInputInOutput() {
		final PGraphics graphics = sketch.createGraphics(sketch.inputImage.width * 16, sketch.inputImage.height * 16);
		graphics.beginDraw();
		graphics.image(sketch.gui.getInputZone(), 0, 0);
		graphics.endDraw();
		
		sketch.output = graphics;
	}
}
