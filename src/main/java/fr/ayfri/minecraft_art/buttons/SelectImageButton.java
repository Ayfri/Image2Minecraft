package fr.ayfri.minecraft_art.buttons;

import fr.ayfri.minecraft_art.Main;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;

public class SelectImageButton extends Button {
	public SelectImageButton(final Main sketch) {
		super(sketch, sketch.width / 2, sketch.height - 100, 150, 80);
		setText("Select Image");
	}
	
	@Override
	public void onClick() {
		final JFileChooser fileChooser = new JFileChooser(FileSystemView.getFileSystemView().getParentDirectory(sketch.input));
		final int returnValue = fileChooser.showOpenDialog(null);
		
		if (returnValue == JFileChooser.APPROVE_OPTION) {
			sketch.input = fileChooser.getSelectedFile();
			sketch.inputImage = sketch.loadImage(fileChooser.getSelectedFile().getAbsolutePath());
		}
	}
}
