package dev.aaronhowser.apps.knome.listener

import dev.aaronhowser.apps.knome.KnomeBot
import dev.aaronhowser.apps.knome.lifecycle.StopCommand
import dev.aaronhowser.apps.knome.quote.QuoteCommand
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class CommandListener : ListenerAdapter() {

	private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
		KnomeBot.LOGGER.severe("Command failed: ${exception.stackTraceToString()}")
	}

	private val commandScope = CoroutineScope(SupervisorJob() + Dispatchers.IO + exceptionHandler)

	override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
		when (event.name) {
			QuoteCommand.COMMAND_NAME -> commandScope.launch { QuoteCommand.handleQuote(event) }
			StopCommand.COMMAND_NAME -> StopCommand.handleStop(event)
		}
	}
}