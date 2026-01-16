package org.example.dev.aaronhowser.apps.knome.command

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands

class CommandListener : ListenerAdapter() {

	override fun onSlashCommandInteraction(event: SlashCommandInteractionEvent) {
		if (event.name == CROSSPOST_COMMAND) {
			event.deferReply(true).queue()
			CoroutineScope(Dispatchers.IO).launch {
				CrosspostCommand.handleCrosspost(event)
			}
		}
	}

	override fun onReady(event: ReadyEvent) {
		event.jda.updateCommands()
			.addCommands(
				Commands.slash(CROSSPOST_COMMAND, "Crosspost messages")
					.addOption(OptionType.STRING, "start", "First message link", true)
					.addOption(OptionType.STRING, "end", "Last message link", false)
			)
			.queue()
	}

	companion object {
		const val CROSSPOST_COMMAND = "crosspost"
	}

}