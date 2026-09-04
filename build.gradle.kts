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

/** The benchmark lives outside `main` so it never ships in the jar, but it needs the app classes and their deps. */
sourceSets.create("bench") {
	compileClasspath += sourceSets.main.get().output
	runtimeClasspath += sourceSets.main.get().output
}

configurations["benchImplementation"].extendsFrom(configurations.implementation.get())
configurations["benchRuntimeOnly"].extendsFrom(configurations.runtimeOnly.get())

kotlin {
	jvmToolchain(25)

	compilerOptions {
		extraWarnings = true
		progressiveMode = true
	}
}

application {
	mainClass = "io.github.ayfri.minecraft_art.MainKt"
	applicationDefaultJvmArgs = listOf(
		/** The native file picker goes through the FFM API, which warns on every restricted call unless the module opts in. */
		"--enable-native-access=ALL-UNNAMED",
		/** A dense output is hundreds of megabytes of pixels, and the default quarter of the machine's memory runs out early. */
		"-XX:MaxRAMPercentage=70",
	)
}

tasks.jar {
	manifest.attributes["Implementation-Title"] = "Image2Minecraft"
	manifest.attributes["Enable-Native-Access"] = "ALL-UNNAMED"
}

tasks.shadowJar {
	/** Shadow merges the `.kotlin_module` metadata itself, so duplicates must reach its transformer instead of being dropped. */
	duplicatesStrategy = DuplicatesStrategy.INCLUDE
}


/** Timings for the generation pipeline, `gradlew bench` after the app has extracted a palette at least once. */
tasks.register<JavaExec>("bench") {
	group = "verification"
	mainClass = "io.github.ayfri.minecraft_art.bench.BenchmarkKt"
	classpath = sourceSets["bench"].runtimeClasspath
	jvmArgs("-XX:MaxRAMPercentage=70")
}

/** Timings for the display path, `gradlew blitBench`, no palette needed since it works on synthetic outputs. */
tasks.register<JavaExec>("blitBench") {
	group = "verification"
	mainClass = "io.github.ayfri.minecraft_art.bench.BlitBenchmarkKt"
	classpath = sourceSets["bench"].runtimeClasspath
	jvmArgs("-XX:MaxRAMPercentage=70", "-Djava.awt.headless=true")
}
