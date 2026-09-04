package io.github.ayfri.minecraft_art.ui

enum class Icon { FOLDER, SAVE, GENERATE, COPY, PASTE, LIST, REFRESH, ZOOM_FIT, GRID, TRASH, IMAGE, CHEVRON, CHECK }

/**
 * Draws [icon] centered on ([centerX], [centerY]) using only [Renderer] primitives, so icons need no asset and no
 * backend-specific path support.
 */
fun Renderer.icon(icon: Icon, centerX: Float, centerY: Float, size: Float, color: Int) {
	val half = size / 2f
	val weight = maxOf(1.4f, size / 10f)
	fun box(inset: Float, radius: Float = 2f) =
		Rect(centerX - half + inset, centerY - half + inset, size - inset * 2f, size - inset * 2f)

	when (icon) {
		Icon.FOLDER -> {
			fill(Rect(centerX - half, centerY - half + size * 0.1f, size * 0.45f, size * 0.18f), color, 2f)
			fill(Rect(centerX - half, centerY - half + size * 0.22f, size, size * 0.62f), color, 3f)
		}

		Icon.SAVE -> {
			line(centerX, centerY - half + size * 0.05f, centerX, centerY + size * 0.15f, color, weight)
			polygon(
				floatArrayOf(
					centerX - size * 0.26f, centerY + size * 0.04f,
					centerX + size * 0.26f, centerY + size * 0.04f,
					centerX, centerY + size * 0.34f,
				), color
			)
			line(centerX - half * 0.8f, centerY + half * 0.86f, centerX + half * 0.8f, centerY + half * 0.86f, color, weight)
		}

		Icon.GENERATE -> {
			polygon(
				floatArrayOf(
					centerX, centerY - half,
					centerX + size * 0.16f, centerY - size * 0.16f,
					centerX + half, centerY,
					centerX + size * 0.16f, centerY + size * 0.16f,
					centerX, centerY + half,
					centerX - size * 0.16f, centerY + size * 0.16f,
					centerX - half, centerY,
					centerX - size * 0.16f, centerY - size * 0.16f,
				), color
			)
		}

		Icon.COPY -> {
			stroke(Rect(centerX - half, centerY - half, size * 0.72f, size * 0.72f), color, 3f, weight)
			fill(Rect(centerX - half + size * 0.28f, centerY - half + size * 0.28f, size * 0.72f, size * 0.72f), color, 3f)
		}

		Icon.PASTE -> {
			stroke(box(0f, 3f), color, 3f, weight)
			fill(Rect(centerX - size * 0.24f, centerY - half - size * 0.06f, size * 0.48f, size * 0.22f), color, 2f)
		}

		Icon.LIST -> for (row in 0..2) {
			val y = centerY - half + size * (0.2f + row * 0.3f)
			fill(Rect(centerX - half, y - weight / 2f, weight * 1.4f, weight), color, 1f)
			line(centerX - half + size * 0.3f, y, centerX + half, y, color, weight)
		}

		Icon.REFRESH -> {
			stroke(box(weight / 2f, half), color, half, weight)
			polygon(
				floatArrayOf(
					centerX + size * 0.08f, centerY - half + size * 0.08f,
					centerX + half + size * 0.06f, centerY - half + size * 0.08f,
					centerX + size * 0.32f, centerY - half + size * 0.44f,
				), color
			)
		}

		Icon.ZOOM_FIT -> {
			val corner = size * 0.34f
			for (dx in intArrayOf(-1, 1)) for (dy in intArrayOf(-1, 1)) {
				val x = centerX + half * dx
				val y = centerY + half * dy
				line(x, y, x - corner * dx, y, color, weight)
				line(x, y, x, y - corner * dy, color, weight)
			}
		}

		Icon.GRID -> {
			stroke(box(0f, 2f), color, 2f, weight)
			line(centerX, centerY - half, centerX, centerY + half, color, weight * 0.8f)
			line(centerX - half, centerY, centerX + half, centerY, color, weight * 0.8f)
		}

		Icon.TRASH -> {
			line(centerX - half, centerY - half + size * 0.18f, centerX + half, centerY - half + size * 0.18f, color, weight)
			fill(Rect(centerX - size * 0.34f, centerY - half + size * 0.28f, size * 0.68f, size * 0.7f), color, 2f)
		}

		Icon.IMAGE -> {
			stroke(box(0f, 3f), color, 3f, weight)
			circle(centerX - size * 0.16f, centerY - size * 0.14f, size * 0.11f, color)
			polygon(
				floatArrayOf(
					centerX - half + weight, centerY + half - weight,
					centerX + size * 0.02f, centerY - size * 0.06f,
					centerX + half - weight, centerY + half - weight,
				), color
			)
		}

		Icon.CHEVRON -> {
			line(centerX - size * 0.34f, centerY - size * 0.14f, centerX, centerY + size * 0.2f, color, weight)
			line(centerX, centerY + size * 0.2f, centerX + size * 0.34f, centerY - size * 0.14f, color, weight)
		}

		Icon.CHECK -> {
			line(centerX - size * 0.34f, centerY + size * 0.02f, centerX - size * 0.06f, centerY + size * 0.3f, color, weight)
			line(centerX - size * 0.06f, centerY + size * 0.3f, centerX + size * 0.36f, centerY - size * 0.28f, color, weight)
		}
	}
}
