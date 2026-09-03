package io.github.ayfri.minecraft_art

import processing.core.PImage

/** Registry of every usable block texture, filled once by [TextureManager.init] and read by the generation pass. */
data object Blocks {
	val textures = mutableMapOf<String, PImage>()
	val colors = mutableMapOf<String, Int>()
	var used = mutableMapOf<String, Int>()

	val size get() = textures.size

	fun register(name: String, texture: PImage, averageColor: Int) {
		textures[name] = texture
		colors[name] = averageColor
	}

	fun countUse(name: String) {
		used[name] = (used[name] ?: 0) + 1
	}

	fun resetUses() {
		used = mutableMapOf()
	}
}
