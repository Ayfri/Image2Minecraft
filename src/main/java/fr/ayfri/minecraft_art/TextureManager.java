package fr.ayfri.minecraft_art;

import processing.core.PImage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Objects;

public class TextureManager {
	private final Main sketch;
	public static final String[] EXCEPTIONS = { "top", "bottom", "brewing_stand", "cauldron", "hopper", "slab", "shulker", "anvil", "wheat" };
	
	// todo Exceptions gérées par le JSON
	public TextureManager(final Main sketch) {
		this.sketch = sketch;
	}
	
	public void init() throws IOException, URISyntaxException {
		final File blocks = ResourceUtils.getLocalFile("./blocks");
		final File[] files = Objects.requireNonNull(blocks).listFiles();
		
		assert files != null;
		for (final File blockFile : files) {
			if (blockFile.getName().endsWith(".png") && Utils.listNotContainsItem(EXCEPTIONS, blockFile.getName())) {
				final PImage image = getImage(blockFile);
				final int averageColor = Utils.getAverageColor(image);
				boolean cancelRegister = false;
				image.loadPixels();
				
				if (image.height != 16 || image.width != 16) {
					continue;
				}
				
				for (final int pixel : image.pixels) {
					final float colorDistance = Utils.getColorDistance(sketch.g, pixel, averageColor);
					if (colorDistance > 60 || sketch.alpha(pixel) < 255) {
						cancelRegister = true;
						break;
					}
				}
				
				if (cancelRegister) {
					continue;
				}
				
				Main.BLOCKS.put(blockFile.getName(), image);
				Main.COLORS_OF_BLOCKS.put(blockFile.getName(), averageColor);
			}
		}
	}
	
	public PImage getImage(final File file) throws FileNotFoundException {
		if (file.exists()) {
			return sketch.loadImage(file.getPath());
		} else {
			throw new FileNotFoundException("File " + file.getPath() + " not found.");
		}
	}
}
