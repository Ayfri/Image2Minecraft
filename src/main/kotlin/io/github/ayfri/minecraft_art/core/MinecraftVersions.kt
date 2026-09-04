package io.github.ayfri.minecraft_art.core

import java.nio.file.Path
import java.util.zip.ZipFile
import kotlin.io.path.deleteExisting
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.outputStream

data class MinecraftVersion(val id: String, val jar: Path, val lastModified: Long)

/** Finds locally installed Minecraft jars and extracts their block textures into the app cache. */
data object MinecraftVersions {
	/**
	 * Secondary faces of a block, every one of them duplicates a face already kept under the plain block name. Anything
	 * that is not a full cube (plants, panes, torches) is dropped later by the opacity check of [BlockPalette].
	 */
	private val EXCLUDED_SUFFIX = Regex(
		""".*(_top|_bottom|_end|_front|_back|_inner|_lit|_on|_off|_open|_side\d|_overlay|_powered|_active|_inactive|_dial|_stage\d*|_upper|_lower|_up|_down|_north|_south|_east|_west)"""
	)
	/** Faces that look like a full cube but belong to a block that never covers one, plus per-state duplicates. */
	private val EXCLUDED_PARTS = listOf(
		"destroy_", "debug", "_bed_", "potted_", "shulker", "_pane_", "item_frame", "hopper", "brewing_stand",
		"cauldron", "comparator", "repeater", "daylight_detector", "lectern", "chorus", "_inside", "slab",
		"piston_top", "_vertical", "farmland", "trapdoor", "fence", "_particle", "anvil", "_awake", "_dormant",
		"_triggered", "_crafting", "conduit", "campfire", "candle", "sign", "banner", "_bloom", "bamboo_stalk",
		"suspicious_", "grass_block_snow", "creaking_heart_top", "carpet",
	)

	/**
	 * Since 2026 releases are named after the year, `26.2` instead of `1.21.x`, with previews suffixed
	 * `-snapshot-N`, `-pre-N` or `-rc-N`. Both schemes parse with the same shape, and the year based ids naturally
	 * rank above the legacy ones.
	 */
	private val RELEASE = Regex("""(\d+)\.(\d+)(?:\.(\d+))?""")
	private val PREVIEW = Regex("""(\d+)\.(\d+)(?:\.(\d+))?-(?:snapshot|pre|rc)-(\d+)""")
	private val SNAPSHOT = Regex("""(\d{2})w(\d{2})([a-z])""")

	/** Custom launchers move the game elsewhere, `MINECRAFT_DIR` lets the user point the app at any install. */
	val gameDirectory: Path = System.getenv("MINECRAFT_DIR")?.let(Path::of)
		?: Path.of(System.getenv("APPDATA") ?: System.getProperty("user.home"), ".minecraft")

	fun installed(): List<MinecraftVersion> {
		val versions = gameDirectory / "versions"
		if (!versions.isDirectory()) return emptyList()

		return versions.listDirectoryEntries()
			.filter(Path::isDirectory)
			.mapNotNull { directory ->
				val jar = directory / "${directory.name}.jar"
				if (!jar.exists()) return@mapNotNull null
				MinecraftVersion(directory.name, jar, jar.getLastModifiedTime().toMillis())
			}
			.sortedWith(compareByDescending<MinecraftVersion> { rank(it.id) }.thenByDescending(MinecraftVersion::lastModified))
	}

	/** Ranks a version id so releases come first, then previews, then old style snapshots, each group newest first. */
	private fun rank(id: String): Long {
		RELEASE.matchEntire(id)?.destructured?.let { (major, minor, patch) ->
			return 4_000_000_000L + major.toLong() * 1_000_000L + minor.toLong() * 1_000L + (patch.toLongOrNull() ?: 0L)
		}
		PREVIEW.matchEntire(id)?.destructured?.let { (major, minor, patch, build) ->
			return 3_500_000_000L + major.toLong() * 1_000_000L + minor.toLong() * 1_000L +
				(patch.toLongOrNull() ?: 0L) * 100L + build.toLong()
		}
		SNAPSHOT.matchEntire(id)?.destructured?.let { (year, week, revision) ->
			return 3_000_000_000L + year.toLong() * 10_000L + week.toLong() * 100L + revision.first().code
		}
		return 0L
	}

	/**
	 * Extracts the block textures of [version] into the app cache, skipping animated textures and entries already
	 * present with the same size. Returns the number of textures available afterwards.
	 */
	fun extractTextures(version: MinecraftVersion, onProgress: (Float) -> Unit = {}): Int {
		val target = AppPaths.textures(version.id)

		ZipFile(version.jar.toFile()).use { jar ->
			val entries = jar.entries().asSequence().filter { it.name.startsWith(TEXTURE_ROOT) }.toList()
			/** A `.mcmeta` also carries mipmap hints, only the ones declaring an animation mean a multi-frame texture. */
			val animated = entries
				.filter { it.name.endsWith(".png.mcmeta") }
				.filter { entry -> jar.getInputStream(entry).use { "\"animation\"" in it.readBytes().decodeToString() } }
				.mapTo(mutableSetOf()) { it.name.removeSuffix(".mcmeta") }
			val textures = entries.filter { it.name.endsWith(".png") && it.name !in animated && keep(it.name) }

			textures.forEachIndexed { index, entry ->
				val destination = target / entry.name.removePrefix(TEXTURE_ROOT)
				if (!destination.exists() || destination.fileSize() != entry.size) {
					jar.getInputStream(entry).use { stream -> destination.outputStream().use(stream::copyTo) }
				}
				onProgress((index + 1f) / textures.size)
			}

			/** Filters change between app versions, so a cache filled by an older run can hold faces we no longer want. */
			val expected = textures.mapTo(mutableSetOf()) { it.name.removePrefix(TEXTURE_ROOT) }
			target.listDirectoryEntries("*.png").filter { it.name !in expected }.forEach(Path::deleteExisting)
		}
		return target.listDirectoryEntries("*.png").size
	}

	private fun keep(name: String): Boolean {
		val file = name.removePrefix(TEXTURE_ROOT).removeSuffix(".png")
		if ('/' in file) return false
		return !EXCLUDED_SUFFIX.matches(file) && EXCLUDED_PARTS.none { it in file }
	}

	private const val TEXTURE_ROOT = "assets/minecraft/textures/block/"
}
