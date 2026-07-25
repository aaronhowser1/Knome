plugins {
	kotlin("jvm") version "2.2.20"
	kotlin("plugin.serialization") version "2.2.20"
	application
}

group = "dev.aaronhowser"
version = "1.0"

repositories {
	mavenCentral()
}

dependencies {
	val coroutinesVersion = property("coroutines_version")
	val jdaVersion = property("jda_version")

	implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
	implementation("net.dv8tion:JDA:$jdaVersion")

	implementation("org.mongodb:mongodb-driver-sync:5.1.0")
}

application {
	mainClass.set("dev.aaronhowser.apps.knome.KnomeBot")
}

tasks.register<Jar>("createFatJar") {
	archiveClassifier.set("all")
	duplicatesStrategy = DuplicatesStrategy.EXCLUDE
	from(sourceSets.main.get().output)
	dependsOn(configurations.runtimeClasspath)
	from({
		configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
	})

	manifest {
		attributes["Main-Class"] = "dev.aaronhowser.apps.knome.KnomeBot"
	}
}