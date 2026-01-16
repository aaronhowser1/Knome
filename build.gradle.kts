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
	implementation("net.dv8tion:JDA:${property("jda_version")}")
}

tasks.test {
	useJUnitPlatform()
}
kotlin {
	jvmToolchain(21)
}