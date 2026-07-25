package dev.aaronhowser.apps.knome.listener

import dev.aaronhowser.apps.knome.KnomeBot
import dev.aaronhowser.apps.knome.crosspost.CrosspostCommand
import dev.aaronhowser.apps.knome.crosspost.CrosspostQueueCommand
import dev.aaronhowser.apps.knome.crosspost.CrosspostStatusCommand
import dev.aaronhowser.apps.knome.lifecycle.StopCommand
import dev.aaronhowser.apps.knome.quote.QuoteCommand
import kotlinx.coroutines.*
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class CommandListener : ListenerAdapter() {

	private val exceptionHandler: CoroutineExceptionHandler =
		CoroutineExceptionHandler { _, exception ->
			KnomeBot.LOGGER.severe("Command failed: ${exception.stackTraceToString()}")
		}

	private val commandScope: CoroutineScope =
		CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)

	override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
		when (event.name) {
			CrosspostCommand.COMMAND_NAME -> {
				commandScope.launch {
					CrosspostCommand.handleCrosspost(event)
				}
			}

			CrosspostStatusCommand.COMMAND_NAME -> {
				commandScope.launch {
					CrosspostStatusCommand.handle(event)
				}
			}

			CrosspostQueueCommand.NEXT_COMMAND_NAME -> {
				commandScope.launch {
					CrosspostQueueCommand.handleNext(event)
				}
			}

			QuoteCommand.COMMAND_NAME -> {
				commandScope.launch {
					QuoteCommand.handleQuote(event)
				}
			}

			StopCommand.COMMAND_NAME -> {
				StopCommand.handleStop(event)
			}
		}
	}

	override fun onMessageContextInteraction(event: MessageContextInteractionEvent) {
		when (event.name) {
			CrosspostCommand.MESSAGE_COMMAND_NAME -> {
				commandScope.launch {
					CrosspostCommand.handleMessageCrosspost(event)
				}
			}

			CrosspostQueueCommand.SKIP_COMMAND_NAME -> {
				commandScope.launch {
					CrosspostQueueCommand.handleSkip(event)
				}
			}
		}
	}

	override fun onModalInteraction(event: ModalInteractionEvent) {
		if (!event.modalId.startsWith("crosspost-range:")) {
			return
		}

		commandScope.launch {
			CrosspostCommand.handleRangeModal(event)
		}
	}

	override fun onReady(event: ReadyEvent) {
		event.jda.updateCommands()
			.addCommands(
				CrosspostCommand.getCommand(),
				CrosspostCommand.getMessageCommand(),
				CrosspostQueueCommand.getNextCommand(),
				CrosspostQueueCommand.getSkipCommand(),
				CrosspostStatusCommand.getCommand(),
				QuoteCommand.getCommand(),
				StopCommand.getCommand()
			)
			.queue()
	}

}