package dev.aaronhowser.apps.knome.listener

import dev.aaronhowser.apps.knome.KnomeBot
import dev.aaronhowser.apps.knome.crosspost.CrosspostCommand
import dev.aaronhowser.apps.knome.lifecycle.StopCommand
import dev.aaronhowser.apps.knome.quote.QuoteCommand
import kotlinx.coroutines.*
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

	override fun onReady(event: ReadyEvent) {
		event.jda.updateCommands()
			.addCommands(
				CrosspostCommand.getCommand(),
				QuoteCommand.getCommand(),
				StopCommand.getCommand()
			)
			.queue()
	}

}
