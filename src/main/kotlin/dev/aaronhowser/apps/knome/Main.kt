package org.example.dev.aaronhowser.apps.knome

import net.dv8tion.jda.api.JDABuilder
import org.example.dev.aaronhowser.apps.knome.command.CommandListener
import org.example.dev.aaronhowser.apps.knome.context.ContextListener

object Main {

	@JvmStatic
	fun main(args: Array<String>) {
		println("Knome!!!!!!!!!!!")

		val token = System.getenv("KNOME_TOKEN") ?: error("KNOME_TOKEN environment variable not set")

		JDABuilder
			.createDefault(token)
			.addEventListeners(CommandListener())
			.addEventListeners(ContextListener())
			.build()
	}

}