package fr.ayfri.minecraft_art;

import processing.core.PImage;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Objects;

public class TextureManager {
	private final Main sketch;
	
	public TextureManager(Main sketch) {
		this.sketch = sketch;
	}
	
	public void init() throws FileNotFoundException {
		String path = "src\\main\\resources\\blocks";
		File file = new File(path);
		
		final File[] files = file.listFiles();
		for (final File blockFile : Objects.requireNonNull(files)) {
			if (blockFile.getName().endsWith(".png") && !blockFile.getName().contains("top") &&
			    !blockFile.getName().contains("bottom")) {
				final PImage image = getImage(blockFile.getName());
				boolean process = true;
				image.loadPixels();
				
				if (image.height != 16 || image.width != 16) {
					continue;
				}
				for (final int pixel : image.pixels) {
					int averageColor = Utils.getAverageColor(sketch.g, image);
					float colorDistance = Utils.getColorDistance(sketch.g, pixel, averageColor);
					if (colorDistance > 130 || sketch.alpha(pixel) < 255) {
						process = false;
						break;
					}
				}
				
				if (!process) {
					continue;
				}
				Main.blocks.put(blockFile.getName(), image);
			}
		}
	}
	
	public PImage getImage(String name) throws FileNotFoundException {
		String path = "src\\main\\resources\\blocks\\" + name;
		File file = new File(path);
		
		if (file.exists()) {
			return sketch.loadImage(path);
		} else {
			throw new FileNotFoundException("File " + file.getAbsolutePath() + " not found.");
		}
	}
}
