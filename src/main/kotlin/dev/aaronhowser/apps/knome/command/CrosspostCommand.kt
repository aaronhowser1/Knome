package org.example.dev.aaronhowser.apps.knome.command

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

object CrosspostCommand {

	const val COMMAND_NAME = "crosspost"
	const val START_ARGUMENT = "start"
	const val END_ARGUMENT = "end"

	fun getCommand(): SlashCommandData {
		return Commands.slash(COMMAND_NAME, "Crosspost messages")
			.addOption(OptionType.STRING, "start", "First message link", true)
			.addOption(OptionType.STRING, "end", "Last message link", false)
	}

	fun handleCrosspost(event: SlashCommandInteractionEvent) {
		val startLink = event.getOption(START_ARGUMENT)?.asString
		val endLink = event.getOption(END_ARGUMENT)?.asString

		if (startLink == null) {
			event.hook.sendMessage("Start link is required.").queue()
			return
		}

	}

}