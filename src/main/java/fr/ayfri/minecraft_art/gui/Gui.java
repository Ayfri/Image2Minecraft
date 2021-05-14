package fr.ayfri.minecraft_art.gui;

import fr.ayfri.minecraft_art.Main;
import fr.ayfri.minecraft_art.buttons.Button;
import fr.ayfri.minecraft_art.buttons.GenerateButton;
import fr.ayfri.minecraft_art.buttons.SaveButton;
import fr.ayfri.minecraft_art.buttons.SelectImageButton;
import fr.ayfri.minecraft_art.rendering.DrawZone;
import fr.ayfri.minecraft_art.rendering.LoadingBar;
import processing.event.MouseEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Gui {
	private final Main sketch;
	private final ArrayList<Slider> sliders;
	private final HashMap<String, Text> texts;
	private final DrawZone outputZone;
	private final DrawZone inputZone;
	private final LoadingBar loadingBar;
	private final SelectImageButton selectImageButton;
	private final SaveButton saveButton;
	private final GenerateButton generateButton;
	
	public Gui(final Main sketch) {
		this.sketch = sketch;
		texts = new HashMap<>();
		sliders = new ArrayList<>();
		inputZone = new DrawZone(sketch, sketch.width / 2 - (512 + 20), 50);
		outputZone = new DrawZone(sketch, sketch.width / 2 + 20, 50);
		loadingBar = new LoadingBar(sketch, sketch.width / 2 + 20, 562, 512, 15);
		
		generateButton = new GenerateButton(sketch);
		saveButton = new SaveButton(sketch);
		selectImageButton = new SelectImageButton(sketch);
		
		addSlider(new Slider(sketch, sketch.width - 400, sketch.height - 50, 250, 10));
		addText(new Text(sketch, 10, 50, 150, 40), "size");
		addText(new Text(sketch, 10, 80, 150, 40), "width");
		addText(new Text(sketch, 10, 110, 150, 40), "height");
		addText(new Text(sketch, 10, 150, 250, 60), "pixels");
		getText("pixels").setSize(20);
		
		addText(new Text(sketch, sketch.width - 165, 80, 150, 32), "out-width");
		addText(new Text(sketch, sketch.width - 165, 100, 150, 32), "out-height");
		
		addText(new Text(sketch, sketch.width / 2 - 315, -22, 150, 80), "Input");
		addText(new Text(sketch, sketch.width / 2 + 235, -22, 150, 80), "Output");
	}

	public void addSlider(final Slider slider) {
		sliders.add(slider);
	}

	public void addText(final Text text, final String name) {
		text.setText(name);
		texts.put(name, text);
	}
	
	public Text getText(final String name) {
		return texts.get(name);
	}
	
	public void draw() {
		getButtons().forEach(Button::draw);
		sliders.forEach(Slider::draw);
		texts.values().forEach(Text::draw);
		final Thread zones = new Thread() {
			@Override
			public void start() {
				inputZone.draw(sketch.inputImage);
				outputZone.draw(sketch.output);
				loadingBar.show();
			}
		};
		
		zones.start();
	}
	
	public ArrayList<Button> getButtons() {
		return new ArrayList<>(List.of(generateButton, saveButton, selectImageButton));
	}
	
	public void mouseEvent(final MouseEvent event) {
		getButtons().forEach(button -> button.mouseEvent(event));
		if (event.getAction() == MouseEvent.DRAG) {
			for (final Slider slider : sliders) {
				if (!slider.isOn) {
					if (slider.isOver()) {
						slider.update(event.getX());
					}
				} else {
					slider.update(event.getX());
				}
			}
		} else if (event.getAction() == MouseEvent.RELEASE) {
			sliders.forEach(slider -> slider.isOn = false);
		}
	}
	
	public GenerateButton getGenerateButton() {
		return generateButton;
	}
	
	public DrawZone getInputZone() {
		return inputZone;
	}
	
	public LoadingBar getLoadingBar() {
		return loadingBar;
	}
	
	public DrawZone getOutputZone() {
		return outputZone;
	}
	
	public SaveButton getSaveButton() {
		return saveButton;
	}
	
	public SelectImageButton getSelectImageButton() {
		return selectImageButton;
	}
	
	public ArrayList<Slider> getSliders() {
		return sliders;
	}
}
