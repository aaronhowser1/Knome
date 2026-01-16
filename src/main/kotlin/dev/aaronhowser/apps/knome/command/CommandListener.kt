package org.example.dev.aaronhowser.apps.knome.command

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class CommandListener : ListenerAdapter() {

	override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
		when (event.name) {
			CrosspostCommand.COMMAND_NAME -> {
				event.deferReply(true).queue()
				CoroutineScope(Dispatchers.IO).launch {
					CrosspostCommand.handleCrosspost(event)
				}
			}
		}
	}

	override fun onReady(event: ReadyEvent) {
		event.jda.updateCommands()
			.addCommands(CrosspostCommand.getCommand())
			.queue()
	}

}