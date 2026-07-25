package dev.aaronhowser.apps.knome.listener

import dev.aaronhowser.apps.knome.KnomeBot
import dev.aaronhowser.apps.knome.crosspost.CrosspostCommand
import dev.aaronhowser.apps.knome.crosspost.CrosspostStatusCommand
import dev.aaronhowser.apps.knome.lifecycle.StopCommand
import dev.aaronhowser.apps.knome.quote.QuoteCommand
import kotlinx.coroutines.*
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent
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
		if (event.name != CrosspostCommand.MESSAGE_COMMAND_NAME) {
			return
		}

		commandScope.launch {
			CrosspostCommand.handleMessageCrosspost(event)
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

	override fun onButtonInteraction(event: ButtonInteractionEvent) {
		if (!event.componentId.startsWith("crosspost:")) {
			return
		}
		commandScope.launch {
			CrosspostCommand.handleButton(event)
		}
	}

	override fun onReady(event: ReadyEvent) {
		event.jda.updateCommands()
			.addCommands(
				CrosspostCommand.getCommand(),
				CrosspostCommand.getMessageCommand(),
				CrosspostStatusCommand.getCommand(),
				QuoteCommand.getCommand(),
				StopCommand.getCommand()
			)
			.queue()
	}

}