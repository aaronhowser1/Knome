package org.example.dev.aaronhowser.apps.knome.command

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.example.dev.aaronhowser.apps.knome.util.AaronServerConstants
import org.example.dev.aaronhowser.apps.knome.util.ExtensionFunctions.await

object CrosspostCommand {

	const val COMMAND_NAME = "crosspost"
	const val START_ARGUMENT = "start"
	const val END_ARGUMENT = "end"

	fun getCommand(): SlashCommandData {
		return Commands.slash(COMMAND_NAME, "Crosspost messages")
			.addOption(OptionType.STRING, "start", "First message ID", true)
			.addOption(OptionType.STRING, "end", "Last message ID", false)
	}

	fun handleCrosspost(event: SlashCommandInteractionEvent) {
		val startId = event.getOption(START_ARGUMENT)?.asLong

		if (startId == null) {
			event.hook.sendMessage("Start id is required.").queue()
			return
		}

		val endId = event.getOption(END_ARGUMENT)?.asLong ?: startId
		val channel = event.channel

		CoroutineScope(Dispatchers.IO).launch {
			val messages = fetchMessagesBetween(
				channel,
				startId,
				endId
			)

			val combinedText = messages
				.joinToString("\n") { it.contentDisplay }
				.trim()

			event.hook.sendMessage("```\n$combinedText\n```").queue()

			postToModlog(event.jda, combinedText)
		}
	}

	private fun postToModlog(jda: JDA, content: String) {
		val modLogChannel = jda.getTextChannelById(AaronServerConstants.MODLOG_CHANNEL_ID) ?: return

		val embed = EmbedBuilder()
			.setTitle("Cross-post")
			.setColor(0x7289DA)
			.setDescription("Messages reposted from Discord:\n\n$content")
			.addField("Tumblr", "todo", true)
			.addField("Bluesky", "todo", true)
			.setFooter("Knome Bot")
			.build()

		modLogChannel
			.sendMessageEmbeds(embed)
			.queue()
	}

	private suspend fun fetchMessagesBetween(
		channel: MessageChannel,
		startMessageId: Long,
		endMessageId: Long
	): List<Message> {
		return withContext(Dispatchers.IO) {
			val minId = minOf(startMessageId, endMessageId)
			val maxId = maxOf(startMessageId, endMessageId)

			val collectedMessages = mutableListOf<Message>()
			var lastMessageId = maxId

			val lastMessage = channel
				.retrieveMessageById(maxId)
				.await()

			collectedMessages.add(lastMessage)

			while (true) {
				val batch = channel
					.getHistoryBefore(lastMessageId, 100)
					.await()
					.retrievedHistory

				if (batch.isEmpty()) break

				for (msg in batch) {
					if (msg.idLong < minId) {
						return@withContext collectedMessages.reversed()
					}

					collectedMessages.add(msg)
				}

				lastMessageId = batch.last().idLong
			}

			return@withContext collectedMessages.reversed()
		}
	}

	data class MessageReference(val guildId: Long, val channelId: Long, val messageId: Long) {
		companion object {
			fun fromLink(link: String): MessageReference {
				val parts = link
					.removePrefix("https://discord.com/channels/")
					.split("/")

				return MessageReference(
					guildId = parts[0].toLong(),
					channelId = parts[1].toLong(),
					messageId = parts[2].toLong()
				)
			}
		}
	}

}