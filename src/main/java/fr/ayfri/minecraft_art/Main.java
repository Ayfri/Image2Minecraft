package fr.ayfri.minecraft_art;

import fr.ayfri.minecraft_art.gui.Gui;
import fr.ayfri.minecraft_art.rendering.LoadingBar;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.event.MouseEvent;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.TreeMap;

public class Main extends PApplet {
	private TextureManager textureManager;
	private Gui gui;
	public static final HashMap<String, PImage> BLOCKS = new HashMap<>();
	public static final HashMap<String, Integer> COLORS_OF_BLOCKS = new HashMap<>();
	public static HashMap<String, Integer> blocksUsed = new HashMap<>();
	//	public PShader shader;
	public PGraphics output;
	public PImage inputImage = null;
	public File input;
	
	public static void main(final String[] args) {
		final String[] processingArgs = { "Main" };
		final Main main = new Main();
		PApplet.runSketch(processingArgs, main);
	}
	
	@Override
	public void settings() {
		System.out.println("Starting");
		size(1400, 700);
		registerMethod("mouseEvent", this);
		registerMethod("pre", this);
		textureManager = new TextureManager(this);
	}
	
	@Override
	public void setup() {
		input = ResourceUtils.getLocalFile("files/outputColor.png");
		assert input != null;
		System.out.println(input.getAbsolutePath());
		output = createGraphics(16, 16);
		
		gui = new Gui(this);
		gui.getSliders().get(0).setMax(20f);
		
		final Thread init = new Thread(this::init);
		init.start();
		try {
			init.join();
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}

//		if (input.exists()) {
		inputImage = loadImage(input.getPath());
		inputImage.loadPixels();
//		}
		surface.setTitle("Image2Minecraft by Ayfri");
		try {
			ResourceUtils.resetBlocks();
		} catch (final URISyntaxException | IOException e) {
			e.printStackTrace();
		}
		System.out.println("Init ended");
	}
	
	@Override
	public void draw() {
		background(180);
		gui.draw();
	}
	
	public void init() {
		try {
			textureManager.init();
			System.out.println("Blocks loaded : " + BLOCKS.size());
		} catch (final IOException | URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
	public void chooseFile(final File file) {
		if (file == null) {
			return;
		}
		this.inputImage = this.loadImage(file.getAbsolutePath());
	}
	
	public void pre() {
		final float ratio = gui.getSliders().get(0).getValue();
		gui.getText("size").setText("Size : " + input.length() / (1024) + " KB");
		gui.getText("width").setText("Width : " + inputImage.width + " px");
		gui.getText("height").setText("Height : " + inputImage.height + " px");
		gui.getText("pixels").setText("Number of \npixels : " + inputImage.width * inputImage.height);
		
		gui.getText("out-width").setText("Width : " + (int) (inputImage.width * ratio) + " blocks");
		gui.getText("out-height").setText("Height : " + (int) (inputImage.height * ratio) + " blocks");
	}
	
	public void mouseEvent(final MouseEvent event) {
		final Thread t = new Thread() {
			@Override
			public void start() {
				gui.mouseEvent(event);
			}
		};
		t.start();
	}
	
	public void generate() {
		System.out.println((char) 27 + "[31mStarting Generate process. ");
		final float ratio = gui.getSliders().get(0).getValue();
//		final float ratio = .05f;
		final int blockSize = 16;
		final long start = System.currentTimeMillis();
		final LoadingBar loading = gui.getLoadingBar();
		final PImage resizedImage = inputImage.copy();
		int x = -blockSize;
		int y = 0;
		
		blocksUsed = new HashMap<>();
		resizedImage.resize((int) (inputImage.width * ratio), (int) (inputImage.height * ratio));
		resizedImage.loadPixels();
		output = createGraphics(resizedImage.width * blockSize, resizedImage.height * blockSize);
		System.out.println("Number of pixels : " + resizedImage.pixels.length);
		
		output.beginDraw();
		for (int i = 0; i < resizedImage.pixels.length; i++) {
			x += blockSize;
			if (x >= resizedImage.width * blockSize) {
				x = 0;
				y += blockSize;
			}
			final PImage color = Utils.getNearestResizedBlock(gui.getOutputZone(), resizedImage.pixels[i]);
			output.image(color, x, y, blockSize, blockSize);
			
			loading.setPercentage((int) PApplet.map(i, 0, resizedImage.pixels.length, 0, 100));
			loading.draw();
		}
		output.endDraw();
		loading.beginDraw();
		loading.clear();
		loading.endDraw();
		System.out.println(System.currentTimeMillis() - start + "ms time took.");
		System.out.println("Image proceeded");
		new TreeMap<>(blocksUsed).forEach((key, value) -> System.out.println(key.substring(0, key.length() - 4) + " : " + value));
	}
	
	/**
	 * This is for testing other way of rendering.
	 */
	public void justPutInputInOutput() {
		output = createGraphics(inputImage.width * 16, inputImage.height * 16);
		output.beginDraw();
		output.image(gui.getInputZone(), 0, 0);
		output.endDraw();
	}
	
	public void save(final File file) {
		if (file == null) {
			return;
		}
		output.save(file.getAbsolutePath());
		System.out.println("Image saved !");
	}
}
