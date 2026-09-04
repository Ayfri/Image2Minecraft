package io.github.ayfri.minecraft_art.core

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div

/** Every file the app writes lives under one user-level directory, nothing is ever created next to the jar. */
data object AppPaths {
	val root: Path = Path.of(System.getenv("APPDATA") ?: System.getProperty("user.home"), "Image2Minecraft")
	val textures: Path = root / "textures"
	val settings: Path = root / "settings.properties"

	fun textures(version: String): Path = (textures / version).createDirectories()

	fun ensure(): Path = root.createDirectories()
}
