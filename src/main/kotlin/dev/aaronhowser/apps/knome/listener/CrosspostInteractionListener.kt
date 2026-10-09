package dev.aaronhowser.apps.knome.listener

import dev.aaronhowser.apps.knome.KnomeBot
import dev.aaronhowser.apps.knome.crosspost.CrosspostReactionHandler
import dev.aaronhowser.apps.knome.crosspost.command.CrosspostQueueCommand
import dev.aaronhowser.apps.knome.crosspost.command.CrosspostStatusCommand
import dev.aaronhowser.apps.knome.discord.AaronServer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class CrosspostInteractionListener : ListenerAdapter() {

	private val commandScope = CoroutineScope(
		SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, exception ->
			KnomeBot.LOGGER.severe("Crosspost command failed: ${exception.stackTraceToString()}")
		}
	)

	override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
		when (event.name) {
			CrosspostStatusCommand.COMMAND_NAME -> launch { CrosspostStatusCommand.handle(event) }
			CrosspostQueueCommand.NEXT_COMMAND_NAME -> launch { CrosspostQueueCommand.handleNext(event) }
		}
	}

	override fun onMessageContextInteraction(event: MessageContextInteractionEvent) {
		when (event.name) {
			CrosspostQueueCommand.SKIP_COMMAND_NAME -> launch { CrosspostQueueCommand.handleSkip(event) }
		}
	}

	override fun onModalInteraction(event: ModalInteractionEvent) {
		when {
			event.modalId.startsWith(CrosspostQueueCommand.SKIP_MODAL_PREFIX) -> launch { CrosspostQueueCommand.handleSkipModal(event) }
		}
	}

	override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
		if (event.userIdLong != AaronServer.AARON_MEMBER_ID) {
			return
		}
		if (event.channel.idLong != AaronServer.PHILOSOPHY_CHANNEL_ID) {
			return
		}

		val replyToMostRecent = when (event.emoji.name) {
			REPOST_EMOJI -> false
			REPLY_EMOJI, REPLY_EMOJI_WITHOUT_VARIATION_SELECTOR -> true
			else -> return
		}

		launch { CrosspostReactionHandler.handle(event, replyToMostRecent) }
	}

	override fun onReady(event: ReadyEvent) {
		event.jda.updateCommands()
			.addCommands(
				CrosspostQueueCommand.getNextCommand(),
				CrosspostQueueCommand.getSkipCommand(),
				CrosspostStatusCommand.getCommand()
			)
			.queue()
	}

	private fun launch(block: suspend () -> Unit) {
		commandScope.launch { block() }
	}

	private companion object {
		const val REPOST_EMOJI = "🔁"
		const val REPLY_EMOJI = "☝️"
		const val REPLY_EMOJI_WITHOUT_VARIATION_SELECTOR = "☝"
	}
}