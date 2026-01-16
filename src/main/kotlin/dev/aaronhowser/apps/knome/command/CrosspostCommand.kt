package org.example.dev.aaronhowser.apps.knome.command

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import org.example.dev.aaronhowser.apps.knome.util.ExtensionFunctions.await

object CrosspostCommand {

	const val COMMAND_NAME = "crosspost"
	const val START_ARGUMENT = "start"
	const val END_ARGUMENT = "end"

	fun getCommand(): SlashCommandData {
		return Commands.slash(COMMAND_NAME, "Crosspost messages")
			.addOption(OptionType.STRING, "start", "First message link", true)
			.addOption(OptionType.STRING, "end", "Last message link", false)
	}

	fun handleCrosspost(event: SlashCommandInteractionEvent) {
		val startLink = event.getOption(START_ARGUMENT)?.asString
		val endLink = event.getOption(END_ARGUMENT)?.asString ?: startLink

		if (startLink == null) {
			event.hook.sendMessage("Start link is required.").queue()
			return
		}

		event.deferReply(true).queue()

		CoroutineScope(Dispatchers.IO).launch {
			val firstRef = MessageReference.fromLink(startLink)

			val channel = event.jda
				.getTextChannelById(firstRef.channelId)
				?: return@launch
		}

	}

	suspend fun fetchMessagesBetween(
		channel: MessageChannel,
		startMessageId: Long,
		endMessageId: Long
	): List<Message> {

		return withContext(Dispatchers.IO) {
			val minId = minOf(startMessageId, endMessageId)
			val maxId = maxOf(startMessageId, endMessageId)

			val collectedMessages = mutableListOf<Message>()
			var lastMessageId = maxId

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