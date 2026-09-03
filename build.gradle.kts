plugins {
	kotlin("jvm") version "2.4.10"
	id("com.gradleup.shadow") version "9.6.1"
	application
}

group = "io.github.ayfri"
version = "0.5"

repositories {
	mavenCentral()
	maven("https://jitpack.io")
}

dependencies {
	/** Processing 4 is not published to Maven Central, which still only hosts the abandoned 3.3.7, so the core comes from JitPack. */
	implementation("com.github.micycle1:processing-core-4:4.5.0") {
		/** Everything here renders through PGraphicsJava2D, so the OpenGL backend and its per-platform natives are dead weight. */
		exclude(group = "org.jogamp.jogl")
		exclude(group = "org.jogamp.gluegen")
		/** iText drags BouncyCastle in twice, under the legacy `bouncycastle` group and the current `org.bouncycastle` one. */
		exclude(group = "bouncycastle")
	}
}

kotlin {
	jvmToolchain(25)

	compilerOptions {
		extraWarnings = true
		progressiveMode = true
	}
}

application {
	mainClass = "io.github.ayfri.minecraft_art.MainKt"
}

tasks.jar {
	manifest.attributes["Implementation-Title"] = "Image2Minecraft"
}

tasks.shadowJar {
	/** Shadow merges the `.kotlin_module` metadata itself, so duplicates must reach its transformer instead of being dropped. */
	duplicatesStrategy = DuplicatesStrategy.INCLUDE
}
