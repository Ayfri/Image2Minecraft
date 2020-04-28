package fr.ayfri.minecraft_art;

import fr.ayfri.minecraft_art.buttons.GenerateButton;
import fr.ayfri.minecraft_art.buttons.SaveButton;
import fr.ayfri.minecraft_art.buttons.SelectImageButton;
import processing.awt.PGraphicsJava2D;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.event.MouseEvent;
import processing.opengl.PGraphicsOpenGL;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;

public class Main extends PApplet {
	private TextureManager textureManager;
	private Gui gui;
	public static final HashMap<String, PImage> blocks = new HashMap<>();
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
		textureManager = new TextureManager(this);
	}
	
	public void setup() {
		input = new File("src\\main\\resources\\outputColor.png");
		output = createGraphics(16, 16);
		
		gui = new Gui(this);
		gui.addButton(new GenerateButton(this));
		gui.addButton(new SaveButton(this));
		gui.addButton(new SelectImageButton(this));
		
		final Thread init = new Thread(this::init);
		init.start();
		try {
			init.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		if (input.exists()) {
			inputImage = loadImage(input.getAbsolutePath());
		}
		frame.setName("Image2Minecraft by Ayfri");
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
		System.out.println("Starting Generate process. ");
		DrawZone loading = gui.getLoadingBar();
		final int ratio = 16;
		int x = -ratio;
		int y = 0;
		output = createGraphics(inputImage.width * ratio, inputImage.height * ratio);
		System.out.println("Width : " + inputImage.width);
		System.out.println("Height : " + inputImage.height);
		
		output.beginDraw();
		
		for (int i = 0; i < inputImage.pixels.length; i++) {
			x += ratio;
			if (x >= inputImage.width * ratio) {
				x = 0;
				y += ratio;
			}
			PImage color = Utils.getNearestResizedBlock(this, inputImage.pixels[i]);
			output.image(color, x, y, ratio, ratio);
			
			loading.beginDraw();
			loading.fill(100, 200, 255);
			loading.rect(0,0, (float) i/inputImage.pixels.length * loading.width, loading.height);
			loading.endDraw();
			loading.show();
		}
		output.endDraw();
		loading.clear();
		System.out.println("Image proceeded");
	}
	
	public void save() {
		output.save("src\\main\\resources\\output.png");
		System.out.println("Image saved !");
	}
}
