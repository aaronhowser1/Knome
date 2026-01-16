package org.example.dev.aaronhowser.apps.knome

import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import org.example.dev.aaronhowser.apps.knome.listener.CommandListener
import org.example.dev.aaronhowser.apps.knome.listener.MessageListener

object Main {

	@JvmStatic
	fun main(args: Array<String>) {
		println("Knome!!!!!!!!!!!")

		val token = System.getenv("KNOME_TOKEN") ?: error("KNOME_TOKEN environment variable not set")

		JDABuilder
			.createDefault(
				token,
				GatewayIntent.MESSAGE_CONTENT
			)
			.addEventListeners(
				CommandListener(),
				MessageListener()
			)
			.build()
	}

}