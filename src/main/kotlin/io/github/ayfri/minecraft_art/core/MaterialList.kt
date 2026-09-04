package io.github.ayfri.minecraft_art.core

import java.io.File

/** Shopping list of the blocks a build needs, in the units a player actually carries them in. */
data class Material(val name: String, val count: Int) {
	val stacks get() = count / STACK
	val remainder get() = count % STACK
	val shulkers get() = count / (STACK * SHULKER_SLOTS)

	fun describe() = when {
		count < STACK -> "$count"
		shulkers > 0 -> "$count ($shulkers shulker${plural(shulkers)}, $stacks stacks)"
		else -> "$count ($stacks stack${plural(stacks)} + $remainder)"
	}

	private fun plural(value: Int) = if (value > 1) "s" else ""

	companion object {
		const val STACK = 64
		const val SHULKER_SLOTS = 27
	}
}

fun materials(usage: Map<String, Int>) = usage.entries
	.sortedByDescending(Map.Entry<String, Int>::value)
	.map { (name, count) -> Material(name, count) }

/** Writes the material list as CSV when the target ends with `.csv`, as an aligned text table otherwise. */
fun exportMaterials(file: File, result: GenerationResult, settings: GenerationSettings, version: String) {
	val list = materials(result.usage)
	val content = if (file.extension.equals("csv", ignoreCase = true)) buildString {
		appendLine("block,count,stacks,remainder,shulker_boxes")
		list.forEach { appendLine("${it.name},${it.count},${it.stacks},${it.remainder},${it.shulkers}") }
	} else buildString {
		appendLine("Image2Minecraft material list")
		appendLine("Minecraft version : $version")
		appendLine("Size              : ${result.blocksWide} x ${result.blocksHigh} blocks (${result.blockCount} blocks)")
		appendLine("Dithering         : ${if (settings.dithering) "on" else "off"}")
		appendLine("Distinct blocks   : ${list.size}")
		appendLine()

		val width = list.maxOfOrNull { it.name.length } ?: 0
		list.forEach { appendLine("${it.name.padEnd(width)}  ${it.describe()}") }
	}
	file.writeText(content)
}
