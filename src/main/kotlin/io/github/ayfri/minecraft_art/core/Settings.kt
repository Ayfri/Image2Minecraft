package io.github.ayfri.minecraft_art.core

import java.io.File
import java.util.Properties
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

/** User preferences kept between runs, so the app reopens on the same version, ratio and palette filters. */
data object Settings {
	private val properties = Properties()

	var version: String?
		get() = properties.getProperty("version")
		set(value) = set("version", value)

	var blocksWide: Int
		get() = properties.getProperty("blocksWide")?.toIntOrNull() ?: 128
		set(value) = set("blocksWide", value.toString())

	var dithering: Boolean
		get() = properties.getProperty("dithering").toBoolean()
		set(value) = set("dithering", value.toString())

	var showGrid: Boolean
		get() = properties.getProperty("showGrid").toBoolean()
		set(value) = set("showGrid", value.toString())

	var excluded: Set<BlockTag>
		get() = properties.getProperty("excluded")
			?.split(',')
			?.mapNotNullTo(mutableSetOf()) { name -> BlockTag.entries.firstOrNull { it.name == name } }
			?: setOf(BlockTag.UNOBTAINABLE)
		set(value) = set("excluded", value.joinToString(",", transform = BlockTag::name))

	var imageDirectory: File?
		get() = properties.getProperty("imageDirectory")?.let(::File)?.takeIf(File::isDirectory)
		set(value) = set("imageDirectory", value?.absolutePath)

	var exportDirectory: File?
		get() = properties.getProperty("exportDirectory")?.let(::File)?.takeIf(File::isDirectory)
		set(value) = set("exportDirectory", value?.absolutePath)

	fun load() {
		if (!AppPaths.settings.exists()) return
		runCatching { AppPaths.settings.inputStream().use(properties::load) }
	}

	fun save() = runCatching {
		AppPaths.ensure()
		AppPaths.settings.outputStream().use { properties.store(it, "Image2Minecraft") }
	}.let { }

	private fun set(key: String, value: String?) {
		if (value == null) properties.remove(key) else properties.setProperty(key, value)
		save()
	}
}
