package dev.aaronhowser.apps.knome

import dev.aaronhowser.apps.knome.feature.QuoteRepository
import dev.aaronhowser.apps.knome.listener.CommandListener
import dev.aaronhowser.apps.knome.listener.MessageListener
import dev.aaronhowser.apps.knome.util.AaronServerConstants
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import java.util.logging.Logger

object KnomeBot {

	val LOGGER: Logger = Logger.getLogger(KnomeBot::class.java.name)

	@JvmStatic
	fun main(args: Array<String>) {
		LOGGER.info("Knome!!!!!!!!!!!!!!")

		val token = System.getenv("KNOME_TOKEN") ?: error("KNOME_TOKEN environment variable not set")

		val jda = JDABuilder
			.createDefault(
				token,
				GatewayIntent.MESSAGE_CONTENT,
				GatewayIntent.GUILD_MESSAGES,
				GatewayIntent.GUILD_MEMBERS,
				GatewayIntent.GUILD_MESSAGE_REACTIONS,
				GatewayIntent.DIRECT_MESSAGES,
				GatewayIntent.DIRECT_MESSAGE_REACTIONS,
			)
			.addEventListeners(
				CommandListener(),
				MessageListener()
			)
			.build()

		jda.awaitReady()

		val bots = jda.getTextChannelById(AaronServerConstants.BOTS_CHANNEL_ID)
		if (bots != null) {
			bots.sendMessage("Knome is starting up...").queue()

			val quotesOnline = runBlocking { QuoteRepository.isOnline() }
			if (!quotesOnline) {
				bots.sendMessage("⚠️ Warning: Quote database is offline! Some features may not work.").queue()
			} else {
				bots.sendMessage("✅ Quote database is online.").queue()
			}
		}
	}

}