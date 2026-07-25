package dev.aaronhowser.apps.knome.crosspost

import dev.aaronhowser.apps.knome.discord.AaronServer
import dev.aaronhowser.apps.knome.discord.await
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.components.label.Label
import net.dv8tion.jda.api.components.textinput.TextInput
import net.dv8tion.jda.api.components.textinput.TextInputStyle
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.modals.Modal

object CrosspostQueueCommand {

	const val NEXT_COMMAND_NAME = "next-crosspost"
	const val SKIP_COMMAND_NAME = "Skip crosspost"
	const val SKIP_MODAL_PREFIX = "crosspost-skip:"
	private const val END_MESSAGE_ID = "end-message"

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

		val endInput = TextInput.create(END_MESSAGE_ID, TextInputStyle.SHORT)
			.setPlaceholder("Leave blank to skip only the selected message")
			.setRequired(false)
			.setMaxLength(100)
			.build()
		val modal = Modal.create("$SKIP_MODAL_PREFIX${event.channelId}:${event.target.id}", "Skip crossposts")
			.addComponents(Label.of("Final message ID or link", endInput))
			.build()
		event.replyModal(modal).await()
	}

	suspend fun handleSkipModal(event: ModalInteractionEvent) {
		if (!event.modalId.startsWith(SKIP_MODAL_PREFIX)) {
			return
		}
		if (event.user.idLong != AaronServer.AARON_MEMBER_ID) {
			event.reply("Only Aaron can use crosspost commands.").setEphemeral(true).await()
			return
		}

		event.deferReply(true).await()
		try {
			val modalParts = event.modalId.removePrefix(SKIP_MODAL_PREFIX).split(":", limit = 2)
			require(modalParts.size == 2 && modalParts[0] == event.channelId) {
				"This skip dialog is invalid or was opened in another channel."
			}

			val startMessageId = parseMessageId(modalParts[1])
			val endValue = event.getValue(END_MESSAGE_ID)?.asString
			val endMessageId = if (endValue.isNullOrBlank()) startMessageId else parseMessageId(endValue)
			val messages = DiscordMessageRangeReader.read(event.channel, startMessageId, endMessageId)
			val eligibleMessages = messages.filter { message ->
				message.author.idLong == AaronServer.AARON_MEMBER_ID &&
					(message.contentRaw.isNotBlank() || message.attachments.isNotEmpty())
			}
			require(eligibleMessages.isNotEmpty()) { "The selected range contains no eligible messages from Aaron." }

			for (message in eligibleMessages) {
				CrosspostRepository.markSkipped(event.channelIdLong, message.idLong)
			}

			val firstMessage = eligibleMessages.first()
			val lastMessage = eligibleMessages.last()
			val range = if (firstMessage.idLong == lastMessage.idLong) {
				firstMessage.jumpUrl
			} else {
				"${firstMessage.jumpUrl}\nthrough\n${lastMessage.jumpUrl}"
			}
			event.hook.editOriginal(
				"${eligibleMessages.size} message(s) will be skipped by /$NEXT_COMMAND_NAME:\n$range"
			).await()
		} catch (exception: Exception) {
			event.hook.editOriginal("Could not mark the messages as skipped: ${exception.message}").await()
		}
	}

	private fun parseMessageId(value: String): Long {
		val id = value.trim().substringAfterLast('/')
		return id.toLongOrNull()
			?: throw IllegalArgumentException("Message IDs and links must end in a numeric message ID.")
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