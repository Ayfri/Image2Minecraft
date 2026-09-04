package io.github.ayfri.minecraft_art.core

import java.io.File
import java.util.stream.Collectors
import javax.imageio.ImageIO

/** Reason a block can be filtered out of the palette, every tag is derived from the texture name. */
enum class BlockTag(val label: String) {
	NOISY("Noisy textures"),
	FALLING("Falling blocks"),
	EXPENSIVE("Expensive blocks"),
	UNOBTAINABLE("Unobtainable blocks"),
	GLOWING("Light emitting"),
	FLAMMABLE("Flammable blocks"),
}

/** One usable block face. Plain class because the 16x16 pixel array makes a generated structural `equals` meaningless. */
class BlockTexture(
	val name: String,
	val pixels: IntArray,
	val color: Int,
	val luminance: Float,
	val chromaA: Float,
	val chromaB: Float,
	val tags: Set<BlockTag>,
)

/**
 * Holds every block face extracted from a Minecraft jar and answers nearest-color queries in Oklab. Filtering rebuilds
 * a flat float array so the matching loop stays cache friendly.
 */
class BlockPalette {
	private var all = emptyList<BlockTexture>()
	private var active = emptyList<BlockTexture>()
	private var colors = FloatArray(0)

	var excluded: Set<BlockTag> = emptySet()
		set(value) {
			field = value
			rebuild()
		}

	val size get() = active.size
	val total get() = all.size
	val entries: List<BlockTexture> get() = active

	/** Loads every 16x16 opaque PNG of [directory], returns how many faces made it into the palette. */
	fun load(directory: File): Int {
		val files = directory.listFiles { file: File -> file.isFile && file.name.endsWith(".png") } ?: emptyArray()
		all = files.toList().parallelStream()
			.map(::read)
			.collect(Collectors.toList())
			.filterNotNull()
			.sortedBy(BlockTexture::name)
		rebuild()
		return all.size
	}

	fun texture(index: Int) = active[index]

	/** Index of the closest block to the given sRGB color, or -1 when the palette is empty. */
	fun nearest(red: Int, green: Int, blue: Int): Int {
		if (active.isEmpty()) return -1

		val target = FloatArray(3)
		oklab(red, green, blue, target)
		val luminance = target[0]
		val chromaA = target[1]
		val chromaB = target[2]

		var best = 0
		var bestDistance = Float.MAX_VALUE
		var index = 0
		while (index < colors.size) {
			val deltaL = colors[index] - luminance
			val deltaA = colors[index + 1] - chromaA
			val deltaB = colors[index + 2] - chromaB
			val distance = deltaL * deltaL + deltaA * deltaA + deltaB * deltaB
			if (distance < bestDistance) {
				bestDistance = distance
				best = index / 3
			}
			index += 3
		}
		return best
	}

	private fun rebuild() {
		active = all.filter { texture -> texture.tags.none(excluded::contains) }
		colors = FloatArray(active.size * 3)
		active.forEachIndexed { index, texture ->
			colors[index * 3] = texture.luminance
			colors[index * 3 + 1] = texture.chromaA
			colors[index * 3 + 2] = texture.chromaB
		}
	}

	private fun read(file: File): BlockTexture? {
		val image = runCatching { ImageIO.read(file) }.getOrNull() ?: return null
		if (image.width != TEXTURE_SIZE || image.height != TEXTURE_SIZE) return null

		val pixels = IntArray(TEXTURE_SIZE * TEXTURE_SIZE)
		image.getRGB(0, 0, TEXTURE_SIZE, TEXTURE_SIZE, pixels, 0, TEXTURE_SIZE)
		if (pixels.any { it ushr 24 != 0xFF }) return null

		var red = 0
		var green = 0
		var blue = 0
		for (pixel in pixels) {
			red += pixel shr 16 and 0xFF
			green += pixel shr 8 and 0xFF
			blue += pixel and 0xFF
		}
		val count = pixels.size
		val average = rgb(red / count, green / count, blue / count)

		val spread = pixels.maxOf { rgbDistanceSquared(it, average) }
		val lab = FloatArray(3)
		oklab(red / count, green / count, blue / count, lab)

		val name = file.name.removeSuffix(".png")
		val tags = tagsOf(name) + if (spread > NOISE_THRESHOLD) setOf(BlockTag.NOISY) else emptySet()
		return BlockTexture(name, pixels, average, lab[0], lab[1], lab[2], tags)
	}

	companion object {
		const val TEXTURE_SIZE = 16

		/** Squared RGB spread above which a texture no longer reads as a single flat color once scaled down. */
		private const val NOISE_THRESHOLD = 90 * 90

		/** Gravity affected blocks, per the Minecraft wiki, restricted to the ones that also have a full cube texture. */
		private val FALLING = setOf("sand", "red_sand", "gravel", "dragon_egg")

		/** Blocks whose material cost makes them impractical for a large build. */
		private val EXPENSIVE = setOf(
			"amethyst_block", "ancient_debris", "beacon", "copper_block", "diamond_block", "emerald_block",
			"gold_block", "iron_block", "lapis_block", "netherite_block", "raw_copper_block", "raw_gold_block",
			"raw_iron_block", "redstone_block", "sponge", "wet_sponge",
		)

		/** Blocks a survival player cannot collect, they only exist through commands or the creative inventory. */
		private val UNOBTAINABLE_EXACT = setOf("barrier", "bedrock", "light", "budding_amethyst", "reinforced_deepslate", "end_portal")
		private val UNOBTAINABLE_PARTS = listOf("command_block", "structure_", "jigsaw", "spawner", "vault", "debug", "test_")

		/** Light emitting full cubes, taken from the wiki light table, unlit variants are deliberately absent. */
		private val GLOWING = setOf(
			"glowstone", "sea_lantern", "shroomlight", "magma", "jack_o_lantern", "crying_obsidian",
			"ochre_froglight_side", "verdant_froglight_side", "pearlescent_froglight_side", "sculk_catalyst_side",
			"sculk_catalyst_side_bloom", "redstone_lamp_on",
		)

		private val FLAMMABLE_PARTS = listOf(
			"planks", "wool", "_log", "_wood", "leaves", "hay_block", "bookshelf", "carpet", "bamboo", "dried_kelp",
			"coal_block", "target", "tnt", "mangrove_roots", "beehive", "bee_nest", "scaffolding", "composter",
		)

		fun tagsOf(name: String) = buildSet {
			if (name in FALLING || name.endsWith("concrete_powder") || name.startsWith("suspicious_")) add(BlockTag.FALLING)
			if (name in EXPENSIVE) add(BlockTag.EXPENSIVE)
			if (name in UNOBTAINABLE_EXACT || UNOBTAINABLE_PARTS.any { it in name }) add(BlockTag.UNOBTAINABLE)
			if (name in GLOWING) add(BlockTag.GLOWING)
			if (FLAMMABLE_PARTS.any { it in name } || name.startsWith("log") || name.endsWith("_log")) add(BlockTag.FLAMMABLE)
		}
	}
}
