# Image2Minecraft

Turns any image into Minecraft block art, using the textures of the Minecraft version installed on your machine.

![Codacy Badge](https://api.codacy.com/project/badge/Grade/1221d5e280624cc0902f6d29bb2350ab)

## Features

- **Version picker**: every Minecraft version found in `.minecraft/versions` is listed, newest first, and its block
  textures are extracted straight from the game jar into a local cache. Both the modern `26.2` names and the legacy
  `1.21.x` ones are understood.
- **Perceptual matching**: blocks are picked in the Oklab color space instead of raw RGB, so dark and saturated areas
  keep their nuances.
- **Floyd-Steinberg dithering**, toggleable, for a much closer match on gradients and skin tones.
- **Palette filters**: exclude noisy textures, falling blocks, expensive blocks, unobtainable blocks, light emitting
  blocks or flammable blocks, to end up with a palette you can actually build with in survival.
- **Material list export** as text or CSV, with counts in blocks, stacks and shulker boxes.
- **Modern interface**: dark theme, live previews with zoom, pan and a block grid overlay, drag and drop, clipboard
  paste and copy, native file dialogs, and settings kept between runs.
- **Keyboard shortcuts**: `Ctrl+O` open, `Ctrl+V` paste, `Ctrl+G` generate, `Ctrl+S` save, `Ctrl+C` copy,
  `Ctrl+E` material list, `F` fit the view, `1` zoom to 100%, `G` toggle the block grid.

## Running

```bash
./gradlew run
```

The app opens on the newest installed version and extracts its textures on first use. `MINECRAFT_DIR` overrides the
game directory when the launcher installs it somewhere else. Caches and preferences live in
`%APPDATA%/Image2Minecraft`.

## Command line

```bash
# Open the app directly on an image
./gradlew run --args="path/to/image.png"

# Convert without opening the window, writes <name>-minecraft.png and <name>-materials.txt
./gradlew run --args="--convert path/to/image.png 192 26.2 --dither"

# Print the block palette of a version, with the tags used by the filters
./gradlew run --args="--palette 26.2"
```

## Architecture

The UI is split from its renderer: widgets only talk to the `Renderer` interface and draw with `Bitmap` images, and
`ProcessingRenderer` is the only class that knows about Processing. Swapping in a faster backend means writing one
new implementation of that interface.

- `core` holds the palette, the generator, version discovery and the desktop integration, none of it depends on a UI
  toolkit.
- `ui` holds the widget layer, the theme and the renderer interface.
- `ui/backend` holds the Processing implementation.
