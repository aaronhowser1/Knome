package dev.aaronhowser.apps.knome

import dev.aaronhowser.apps.knome.listener.CommandListener
import dev.aaronhowser.apps.knome.listener.MessageListener
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent

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