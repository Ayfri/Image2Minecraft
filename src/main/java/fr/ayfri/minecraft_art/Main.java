package fr.ayfri.minecraft_art;

import fr.ayfri.minecraft_art.gui.Gui;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.event.MouseEvent;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main extends PApplet {
	public static final HashMap<String, PImage> BLOCKS = new HashMap<>();
	public static final HashMap<String, Integer> COLORS_OF_BLOCKS = new HashMap<>();
	public static HashMap<String, Integer> blocksUsed = new HashMap<>();
	public TextureManager textureManager;
	public Gui gui;
	//	public PShader shader;
	public PGraphics output;
	public PImage inputImage = null;
	public File input;
	
	public ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
		Runtime.getRuntime().availableProcessors(),
		Runtime.getRuntime().availableProcessors() * 2,
		5,
		TimeUnit.MINUTES,
		new LinkedBlockingQueue<>()
	);
	
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
		output = createGraphics(16, 16);
		
		gui = new Gui(this);
		gui.getSliders().get(0).setMax(20f);
		
		final Future<?> init = threadPool.submit(this::init);
		try {
			init.get();
		} catch (final InterruptedException | ExecutionException e) {
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
		threadPool.submit((Callable<Void>) () -> {
			gui.mouseEvent(event);
			return null;
		});
	}
	
	public void save(final File file) {
		if (file == null) {
			return;
		}
		output.save(file.getAbsolutePath());
		System.out.println("Image saved !");
	}
}
