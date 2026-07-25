package dev.aaronhowser.apps.knome

import dev.aaronhowser.apps.knome.discord.AaronServer
import dev.aaronhowser.apps.knome.discord.await
import dev.aaronhowser.apps.knome.listener.CommandListener
import dev.aaronhowser.apps.knome.listener.MessageListener
import dev.aaronhowser.apps.knome.quote.QuoteRepository
import kotlinx.coroutines.runBlocking
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag
import java.util.logging.Logger

object KnomeBot {

	val LOGGER: Logger = Logger.getLogger(KnomeBot::class.java.name)

	@JvmStatic
	fun main(args: Array<String>) {
		LOGGER.info("Knome!!!!!!!!!!!!!!")

		val token = System.getenv("KNOME_TOKEN")
			?: error("KNOME_TOKEN environment variable not set")

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
			.disableCache(
				CacheFlag.VOICE_STATE,
				CacheFlag.EMOJI,
				CacheFlag.STICKER,
				CacheFlag.SCHEDULED_EVENTS,
			)
			.addEventListeners(
				CommandListener(),
				MessageListener()
			)
			.build()

		Runtime.getRuntime()
			.addShutdownHook(Thread {
				QuoteRepository.close()
				jda.shutdown()
			})

		try {
			jda.awaitReady()
			runBlocking {
				sendStartupStatus(jda)
			}
		} catch (exception: Exception) {
			jda.shutdownNow()
			throw exception
		}
	}

	private suspend fun sendStartupStatus(jda: JDA) {
		val botsChannel = jda.getTextChannelById(AaronServer.BOTS_CHANNEL_ID)
		if (botsChannel == null) {
			LOGGER.warning("Bots channel ${AaronServer.BOTS_CHANNEL_ID} was not found")
			return
		}

		val mostRecentMessage = botsChannel.history.retrievePast(1).await().firstOrNull()
		if (mostRecentMessage?.author?.idLong == jda.selfUser.idLong) {
			LOGGER.info("Skipping startup status because the latest message is already from Knome")
			return
		}

		botsChannel.sendMessage("Knome is starting up...").await()

		val databaseStatus = if (QuoteRepository.isOnline()) {
			"✅ Quote database is online."
		} else {
			"⚠️ Warning: Quote database is offline! Some features may not work."
		}
		botsChannel.sendMessage(databaseStatus).await()
	}
}