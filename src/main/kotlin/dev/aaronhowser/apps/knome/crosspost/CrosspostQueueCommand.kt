package dev.aaronhowser.apps.knome.crosspost

import dev.aaronhowser.apps.knome.discord.AaronServer
import dev.aaronhowser.apps.knome.discord.await
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

object CrosspostQueueCommand {

	const val NEXT_COMMAND_NAME = "next-crosspost"
	const val SKIP_COMMAND_NAME = "Skip crosspost"

	fun getNextCommand(): SlashCommandData {
		return Commands.slash(NEXT_COMMAND_NAME, "Find the oldest message that has not been handled")
			.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
	}

	fun getSkipCommand(): CommandData {
		return Commands.message(SKIP_COMMAND_NAME)
			.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
	}

	suspend fun handleNext(event: SlashCommandInteractionEvent) {
		if (event.user.idLong != AaronServer.AARON_MEMBER_ID) {
			event.reply("Only Aaron can use crosspost commands.").setEphemeral(true).await()
			return
		}
		if (event.channelIdLong != AaronServer.PHILOSOPHY_CHANNEL_ID) {
			event.reply("Use this command in #philosophy.").setEphemeral(true).await()
			return
		}

		event.deferReply(true).await()
		try {
			val message = findOldestUnhandledMessage(event.channel)
			if (message == null) {
				event.hook.editOriginal("Every eligible message in #philosophy has been handled.").await()
			} else {
				event.hook.editOriginal("Oldest message waiting to be crossposted: ${message.jumpUrl}").await()
			}
		} catch (exception: Exception) {
			event.hook.editOriginal("Could not check the crosspost queue: ${exception.message}").await()
		}
	}

	suspend fun handleSkip(event: MessageContextInteractionEvent) {
		if (event.user.idLong != AaronServer.AARON_MEMBER_ID) {
			event.reply("Only Aaron can use crosspost commands.").setEphemeral(true).await()
			return
		}
		if (event.channelIdLong != AaronServer.PHILOSOPHY_CHANNEL_ID) {
			event.reply("Only messages in #philosophy can be skipped.").setEphemeral(true).await()
			return
		}
		if (event.target.author.idLong != AaronServer.AARON_MEMBER_ID) {
			event.reply("Only Aaron's messages can be added to the crosspost queue.").setEphemeral(true).await()
			return
		}

		event.deferReply(true).await()
		try {
			CrosspostRepository.markSkipped(event.channelIdLong, event.target.idLong)
			event.hook.editOriginal("This message will be skipped by /$NEXT_COMMAND_NAME:\n${event.target.jumpUrl}").await()
		} catch (exception: Exception) {
			event.hook.editOriginal("Could not mark the message as skipped: ${exception.message}").await()
		}
	}

	private suspend fun findOldestUnhandledMessage(channel: MessageChannel): Message? {
		var lastMessageId: Long? = null

		while (true) {
			val history = if (lastMessageId == null) {
				channel.getHistoryFromBeginning(BATCH_SIZE).await()
			} else {
				channel.getHistoryAfter(lastMessageId, BATCH_SIZE).await()
			}
			val messages = history.retrievedHistory.sortedBy { message -> message.idLong }
			if (messages.isEmpty()) {
				return null
			}

			val eligibleMessages = messages.filter { message ->
				message.author.idLong == AaronServer.AARON_MEMBER_ID &&
					(message.contentRaw.isNotBlank() || message.attachments.isNotEmpty())
			}
			val handledMessageIds = CrosspostRepository.getHandledMessageIds(
				eligibleMessages.map { message -> message.idLong }
			)
			for (message in eligibleMessages) {
				if (message.idLong !in handledMessageIds) {
					return message
				}
			}

			val nextLastMessageId = messages.last().idLong
			if (messages.size < BATCH_SIZE || nextLastMessageId == lastMessageId) {
				return null
			}
			lastMessageId = nextLastMessageId
		}
	}

	private const val BATCH_SIZE = 100
}