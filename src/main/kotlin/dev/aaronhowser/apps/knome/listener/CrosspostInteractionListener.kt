package dev.aaronhowser.apps.knome.listener

import dev.aaronhowser.apps.knome.KnomeBot
import dev.aaronhowser.apps.knome.crosspost.command.CrosspostCommand
import dev.aaronhowser.apps.knome.crosspost.command.CrosspostQueueCommand
import dev.aaronhowser.apps.knome.crosspost.command.CrosspostSeriesCommand
import dev.aaronhowser.apps.knome.crosspost.command.CrosspostStatusCommand
import dev.aaronhowser.apps.knome.crosspost.command.CrosspostThreadCommand
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
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
			CrosspostCommand.COMMAND_NAME -> launch { CrosspostCommand.handleCrosspost(event) }
			CrosspostSeriesCommand.COMMAND_NAME -> launch { CrosspostSeriesCommand.handle(event) }
			CrosspostThreadCommand.COMMAND_NAME -> launch { CrosspostThreadCommand.handle(event) }
			CrosspostCommand.REPLY_THREAD_COMMAND_NAME -> launch { CrosspostCommand.handleReplyThread(event) }
			CrosspostStatusCommand.COMMAND_NAME -> launch { CrosspostStatusCommand.handle(event) }
			CrosspostQueueCommand.NEXT_COMMAND_NAME -> launch { CrosspostQueueCommand.handleNext(event) }
		}
	}

	override fun onMessageContextInteraction(event: MessageContextInteractionEvent) {
		when (event.name) {
			CrosspostCommand.MESSAGE_COMMAND_NAME -> launch { CrosspostCommand.handleMessageCrosspost(event, "combined") }
			CrosspostCommand.SERIES_MESSAGE_COMMAND_NAME -> launch { CrosspostCommand.handleMessageCrosspost(event, "individual") }
			CrosspostCommand.THREAD_MESSAGE_COMMAND_NAME -> launch { CrosspostCommand.handleMessageCrosspost(event, "combined") }
			CrosspostCommand.REPLY_THREAD_MESSAGE_COMMAND_NAME -> launch { CrosspostCommand.handleMessageCrosspost(event, "reply-thread") }
			CrosspostQueueCommand.SKIP_COMMAND_NAME -> launch { CrosspostQueueCommand.handleSkip(event) }
		}
	}

	override fun onModalInteraction(event: ModalInteractionEvent) {
		when {
			event.modalId.startsWith(CrosspostCommand.MODAL_PREFIX) -> launch { CrosspostCommand.handleRangeModal(event) }
			event.modalId.startsWith(CrosspostQueueCommand.SKIP_MODAL_PREFIX) -> launch { CrosspostQueueCommand.handleSkipModal(event) }
		}
	}

	override fun onReady(event: ReadyEvent) {
		event.jda.updateCommands()
			.addCommands(
				CrosspostCommand.getCommand(),
				CrosspostSeriesCommand.getCommand(),
				CrosspostThreadCommand.getCommand(),
				CrosspostCommand.getReplyThreadCommand(),
				CrosspostCommand.getMessageCommand(),
				CrosspostCommand.getSeriesMessageCommand(),
				CrosspostCommand.getThreadMessageCommand(),
				CrosspostCommand.getReplyThreadMessageCommand(),
				CrosspostQueueCommand.getNextCommand(),
				CrosspostQueueCommand.getSkipCommand(),
				CrosspostStatusCommand.getCommand()
			)
			.queue()
	}

	private fun launch(block: suspend () -> Unit) {
		commandScope.launch { block() }
	}
}