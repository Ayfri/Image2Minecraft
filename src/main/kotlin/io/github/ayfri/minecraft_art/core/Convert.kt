package io.github.ayfri.minecraft_art.core

import java.io.File

/**
 * Headless conversion, run with `--convert <image> [width] [version] [--dither]`. Useful to batch a folder from a
 * script without opening the window, and it exercises the exact same generator as the button does.
 */
fun convertImage(arguments: List<String>) {
	val options = arguments.filter { it.startsWith("--") }
	val positional = arguments.filterNot { it.startsWith("--") }
	val input = positional.firstOrNull()?.let(::File)
	if (input == null || !input.isFile) return println("Usage: --convert <image> [width] [version] [--dither]")

	val versions = MinecraftVersions.installed()
	if (versions.isEmpty()) return println("No Minecraft install found in ${MinecraftVersions.gameDirectory}.")

	val width = positional.getOrNull(1)?.toIntOrNull() ?: 128
	val requested = positional.getOrNull(2)
	val version = requested?.let { id -> versions.firstOrNull { it.id == id } } ?: versions.first()

	MinecraftVersions.extractTextures(version)
	val palette = BlockPalette()
	palette.load(AppPaths.textures(version.id).toFile())
	palette.excluded = Settings.excluded

	val source = Platform.readImage(input) ?: return println("Cannot read ${input.name}.")
	val settings = GenerationSettings(width, "--dither" in options)
	val result = Generator(palette).generate(source, settings) ?: return println("The palette is empty.")

	val output = File(input.parentFile, "${input.nameWithoutExtension}-minecraft.png")
	Platform.savePng(output, result.image)
	exportMaterials(File(input.parentFile, "${input.nameWithoutExtension}-materials.txt"), result, settings, version.id)

	println("${version.id} palette : ${palette.size} block faces")
	println("${result.blocksWide} x ${result.blocksHigh} blocks, ${result.blockCount} blocks, ${result.usage.size} types, ${result.elapsed}")
	println("Wrote ${output.absolutePath} (${result.image.width} x ${result.image.height} px)")
}
