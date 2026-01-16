plugins {
	kotlin("jvm") version "2.2.20"
	application
}

group = "dev.aaronhowser"
version = "1.0"

repositories {
	mavenCentral()
}

dependencies {
	val jdaVersion = property("jda_version")
	val ktorVersion = property("ktor_version")
	val serializationVersion = property("kt_serialization_version")

	implementation("net.dv8tion:JDA:$jdaVersion")

	implementation("io.ktor:ktor-client-core:$ktorVersion")
	implementation("io.ktor:ktor-client-cio:$ktorVersion")
	implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
	implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

	implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")

	implementation("org.mongodb:mongodb-driver-sync:5.1.0")
}

application {
	mainClass.set("dev.aaronhowser.apps.knome.Main")
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
		attributes["Main-Class"] = "dev.aaronhowser.apps.knome.Main"
	}
}