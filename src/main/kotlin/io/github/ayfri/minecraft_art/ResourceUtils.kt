package io.github.ayfri.minecraft_art

import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.outputStream

/** Directory the jar itself lives in, every runtime resource is resolved against it. */
val localDirectory: File
	get() = File(Main::class.java.protectionDomain.codeSource.location.toURI()).parentFile

/** Creates [path] as an empty file when it does not exist yet, and returns whether it was already there. */
fun assertFile(path: Path): Boolean {
	val file = localDirectory.toPath().resolve(path).toFile()
	if (file.isFile) return true

	file.parentFile?.mkdirs()
	if (!file.createNewFile()) throw IOException("Cannot create new file to '$path'.")
	println("Created '$path' file.")
	return false
}

fun assertFile(path: String) = assertFile(Path.of(path))

/** Same as [assertFile] but also writes [content] into the file. */
fun assertFile(path: Path, content: String): Boolean {
	val existed = runCatching { assertFile(path) }.getOrDefault(false)
	runCatching { localDirectory.toPath().resolve(path).toFile().writeText(content) }.onFailure(Throwable::printStackTrace)
	return existed
}

/** Creates [path] as a directory when needed, replacing a plain file left there by an older run. */
fun assertDirectory(path: Path): Boolean {
	val directory = localDirectory.toPath().resolve(path).toFile()
	if (directory.isDirectory) return true

	if (directory.isFile) directory.delete()
	if (!directory.mkdirs()) throw IOException("Cannot create directory '$path'.")
	println("Created '$path' directory.")
	return false
}

fun getLocalFile(path: Path) = runCatching {
	assertFile(path)
	localDirectory.toPath().resolve(path).toFile()
}.getOrNull()

fun getLocalFile(path: String) = getLocalFile(Path.of(path))

/** Resolves [path] against [localDirectory] without creating anything, for optional resources. */
fun localFileOrNull(path: String) = localDirectory.toPath().resolve(path).toFile().takeIf(File::isFile)

fun getLocalDirectory(path: Path) = runCatching {
	assertDirectory(path)
	localDirectory.toPath().resolve(path).toFile()
}.getOrNull()

fun getLocalDirectory(path: String) = getLocalDirectory(Path.of(path))

fun resetBlocks() {
	val blocks = getLocalDirectory("blocksTest")?.listFiles() ?: return
	for (file in blocks) {
		if (!file.delete()) println("Can't find file ${file.absolutePath}.")
	}
}

/** Extracts every block texture of a locally installed Minecraft [version] jar into the `blocksTest` directory. */
fun registerBlocksFromVersion(version: String) = runCatching {
	val versions = Path.of(System.getProperty("user.home"), "AppData", "Roaming", ".minecraft", "versions")
	val target = (getLocalDirectory("blocksTest") ?: return@runCatching).toPath()

	ZipFile((versions / version / "$version.jar").toFile()).use { jar ->
		jar.entries().asSequence()
			.filter { "assets/minecraft/textures/block" in it.name && it.name.endsWith(".png") }
			.filter { entry -> TextureManager.EXCEPTIONS.none { it in entry.name } }
			.forEach { entry ->
				/** Entry names all start with `assets/minecraft/textures/block/`, which is exactly 32 characters. */
				val destination = target / entry.name.substring(32)
				if (destination.exists() && destination.fileSize() == entry.size) return@forEach

				destination.parent.toFile().mkdirs()
				jar.getInputStream(entry).use { stream -> destination.outputStream().use(stream::copyTo) }
			}
	}
}.onFailure(Throwable::printStackTrace)
