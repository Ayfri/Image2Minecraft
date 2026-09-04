package io.github.ayfri.minecraft_art.core

/**
 * Headless palette dump, run with `--palette [version]`. It prints what the generator would actually use, which is the
 * quickest way to check a Minecraft version against the block filters without opening the window.
 */
fun printPaletteReport(versionId: String?) {
	val versions = MinecraftVersions.installed()
	if (versions.isEmpty()) return println("No Minecraft install found in ${MinecraftVersions.gameDirectory}.")

	val version = versionId?.let { id -> versions.firstOrNull { it.id == id } } ?: versions.first()
	if (versionId != null && version.id != versionId) return println("Version $versionId is not installed.")

	println("Extracting textures from ${version.id} (${version.jar})")
	val extracted = MinecraftVersions.extractTextures(version)
	println("$extracted texture files cached in ${AppPaths.textures(version.id)}")

	val palette = BlockPalette()
	palette.load(AppPaths.textures(version.id).toFile())
	println("${palette.total} usable block faces after the opacity and size checks")

	BlockTag.entries.forEach { tag ->
		val matching = palette.entries.filter { tag in it.tags }
		println()
		println("${tag.label} (${matching.size}) : ${matching.joinToString(", ", transform = BlockTexture::name)}")
	}

	palette.excluded = setOf(BlockTag.UNOBTAINABLE)
	println()
	println("Palette with the default filters : ${palette.size} faces")
	println(palette.entries.joinToString(", ", transform = BlockTexture::name))
}
