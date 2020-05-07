package fr.ayfri.minecraft_art;

import fr.ayfri.minecraft_art.buttons.GenerateButton;
import fr.ayfri.minecraft_art.buttons.SaveButton;
import fr.ayfri.minecraft_art.buttons.SelectImageButton;
import fr.ayfri.minecraft_art.gui.Gui;
import fr.ayfri.minecraft_art.gui.Slider;
import fr.ayfri.minecraft_art.rendering.DrawZone;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.event.MouseEvent;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;

public class Main extends PApplet {
	private TextureManager textureManager;
	private Gui gui;
	public static final HashMap<String, PImage> blocks = new HashMap<>();
	public static HashMap<String, Integer> blocksUsed = new HashMap<>();
	//	public PShader shader;
	public PGraphics output;
	public PImage inputImage = null;
	public File input;
	
	public static void main(String[] args) {
		String[] processingArgs = { "Main" };
		Main main = new Main();
		PApplet.runSketch(processingArgs, main);
	}
	
	public void settings() {
		System.out.println("Starting");
		size(1400, 700);
		registerMethod("mouseEvent", this);
		registerMethod("pre", this);
		textureManager = new TextureManager(this);
	}
	
	public void setup() {
//		shader = loadShader("src\\main\\resources\\vertex.glsl");
		input = new File("src\\main\\resources\\outputColor.png");
		output = createGraphics(16, 16);
		
		gui = new Gui(this);
		gui.getSliders().get(0).setMax(20f);
		
		final Thread init = new Thread(this::init);
		init.start();
		try {
			init.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if (input.exists()) {
			inputImage = loadImage(input.getAbsolutePath());
			inputImage.loadPixels();
		}
		surface.setTitle("Image2Minecraft by Ayfri");
		System.out.println("Init ended");
	}
	
	public void draw() {
		background(180);
		gui.draw();
	}
	
	public void init() {
		try {
			textureManager.init();
			System.out.println("Blocks loaded : " + blocks.size());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}
	
	public void pre() {
		gui.getText("size").setText("Size : " + input.length() / (1024) + " KB");
		gui.getText("width").setText("Width : " + inputImage.width + " px");
		gui.getText("height").setText("Height : " + inputImage.height + " px");
		gui.getText("pixels").setText("Number of \npixels : " + inputImage.width * inputImage.height);
	}
	
	public void mouseEvent(MouseEvent event) {
		Thread t = new Thread() {
			@Override
			public void start() {
				gui.mouseEvent(event);
			}
		};
		t.start();
	}
	
	public void generate() {
		System.out.println((char) 27 + "[31mStarting Generate process. ");
		float ratio = gui.getSliders().get(0).getValue();
		final int blockSize = 16;
		final long start = System.currentTimeMillis();
		final DrawZone loading = gui.getLoadingBar();
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
			PImage color = Utils.getNearestResizedBlock(gui.getOutputZone(), resizedImage.pixels[i]);
			output.image(color, x, y, blockSize, blockSize);
			
			final float rectSize = (float) i / resizedImage.pixels.length * loading.width;
			loading.beginDraw();
			loading.noStroke();
			loading.fill(100, 200, 255);
			loading.rect(0, 0,  rectSize, loading.height);
			loading.fill(0);
			loading.textSize(12);
			loading.textAlign(CENTER, CENTER);
			loading.text((int) PApplet.map(i,0,resizedImage.pixels.length, 0,100) + "%", rectSize/2f, loading.height/2f);
			loading.endDraw();
			loading.show();
		}
		output.endDraw();
		loading.beginDraw();
		loading.clear();
		loading.endDraw();
		System.out.println(System.currentTimeMillis() - start + "ms time took." + (char) 27 + "[38m");
		System.out.println("Image proceeded");
		blocksUsed.forEach((key1, value) -> System.out.println(key1.substring(0, key1.length() - 4) + " : " + value));
	}
	
	// This is for testing other way of rendering.
	public void justPutInputInOutput() {
		output = createGraphics(inputImage.width * 16, inputImage.height * 16);
		output.beginDraw();
		output.image(gui.getInputZone(), 0, 0);
		output.endDraw();
	}
	
	public void save() {
		output.save("src\\main\\resources\\output.png");
		System.out.println("Image saved !");
	}
}
