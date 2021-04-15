package fr.ayfri.minecraft_art;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ResourceUtils {
	public static void registerColorValues() {
		final File json = new File("files\\colors.json");
	}
	
	public static void resetBlocks() throws URISyntaxException, IOException {
		final File blocks = getLocalFile(Path.of("blocksTest"));
		for (final File file : Objects.requireNonNull(blocks.listFiles())) {
			final boolean stillHere = !file.delete();
			if (stillHere) {
				System.out.println("Can't find file " + file.getAbsolutePath() + ".");
			}
		}
	}
	
	public static File getLocalFile(final String path) {
		return getLocalFile(Path.of(path));
	}
	
	public static File getLocalFile(final Path path) {
		try {
			assertFile(path);
			return getLocalDirectory().toPath().resolve(path).toFile();
		} catch (final URISyntaxException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static boolean assertFile(final String path) throws URISyntaxException, IOException {
		return assertFile(Path.of(path));
	}
	
	public static boolean assertFile(final Path path) throws URISyntaxException, IOException {
		final File file = getLocalDirectory().toPath().resolve(path).toFile();
		boolean result = false;
		if (file.exists()) {
			result = true;
		} else {
			final boolean isFailed = file.isDirectory() ? !file.mkdir() : !file.createNewFile();
			System.out.println("Created '" + (file.isDirectory() ? "' directory" : "' file") + " in '" + path + "'.");
			if (isFailed) {
				throw new IOException("Cannot create new file to '" + path + "'.");
			}
		}
		
		return result;
	}
	
	public static File getLocalDirectory() throws URISyntaxException {
		return new File(ResourceUtils.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
	}
	
	public static boolean assertFile(final Path path, final String content) throws URISyntaxException {
		final File file = getLocalDirectory().toPath().resolve(path).toFile();
		boolean result = false;
		try (final FileWriter writer = new FileWriter(file)) {
			if (file.exists()) {
				writer.write(content);
				result = true;
			} else {
				final boolean isFailed = file.isDirectory() ? !file.mkdir() : !file.createNewFile();
				System.out.println("Created '" + (file.isDirectory() ? "' directory" : "' file") + " in '" + path + "'.");
				if (isFailed) {
					writer.write(content);
				} else {
					throw new IOException("Cannot create new file to '" + path + "'.");
				}
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
		
		return result;
	}
	
	public static void registerBlocksFromVersion(final String version) {
		try (
			final ZipFile jarFile = new ZipFile(System.getProperty("user.home") + "\\AppData\\Roaming\\.minecraft\\versions\\" + version + "\\" + version + ".jar")
		) {
			final Enumeration<? extends ZipEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				final ZipEntry entry = entries.nextElement();
				final String name = entry.getName();
				if (name.contains("assets/minecraft/textures/block") && name.endsWith(".png") && Utils.listNotContainsItem(TextureManager.EXCEPTIONS, name)) {
					final InputStream stream = jarFile.getInputStream(entry);
					final URI uri = Paths.get(getLocalDirectory().getAbsolutePath(), "blocksTest\\" + name.substring(32)).toUri();
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
		} catch (final IOException | URISyntaxException e) {
			e.printStackTrace();
		}
	}
}
