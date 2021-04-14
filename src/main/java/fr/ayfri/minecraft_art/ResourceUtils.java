package fr.ayfri.minecraft_art;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ResourceUtils {
	public static void registerColorValues() {
		final File json = new File("files\\colors.json");
	}
	
	public static void resetBlocks() {
		final File blocks = new File("blocksTest");
		for (final File file : Objects.requireNonNull(blocks.listFiles())) {
			final boolean stillHere = !file.delete();
			if (stillHere) {
				System.out.println("Can't find file " + file.getAbsolutePath() + ".");
			}
		}
	}
	
	public static void registerBlocksFromVersion(final String version) {
		try (
			final ZipFile jarFile = new ZipFile(System.getProperty("user.home") + "\\AppData\\Roaming\\.minecraft\\versions\\" + version + "\\" + version + ".jar")
		) {
			final File dir = new File("blocksTest");
			if (!dir.exists()) {
				dir.mkdir();
			}
			
			final Enumeration<? extends ZipEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				final ZipEntry entry = entries.nextElement();
				final String name = entry.getName();
				if (name.contains("assets/minecraft/textures/block") && name.endsWith(".png") && Utils.listNotContainsItem(TextureManager.EXCEPTIONS, name)) {
					final InputStream stream = jarFile.getInputStream(entry);
					final URI uri = Paths.get("blocksTest\\" + name.substring(32)).toUri();
					final File file = new File(uri);
					
					if (file.exists()) {
						if (file.length() == entry.getSize()) {
							continue;
						}
					}
					
					Files.copy(stream, Paths.get(file.getPath()));
					stream.close();
				}
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
}
