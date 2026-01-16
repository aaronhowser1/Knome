package org.example.dev.aaronhowser.apps.knome.command

import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData

object QuoteCommand {

	const val COMMAND_NAME = "quote"

	const val ADD_SUBCOMMAND = "add"
	const val QUOTEE_ARGUMENT = "quotee"
	const val MESSAGE_ARGUMENT = "message"

	const val GET_SUBCOMMAND = "get"
	const val ID_ARGUMENT = "id"

	const val DELETE_SUBCOMMAND = "delete"
	const val LIST_SUBCOMMAND = "list"

	fun getCommand(): SlashCommandData {
		return Commands.slash(COMMAND_NAME, "Quotes!")
			.addSubcommands(
				SubcommandData(ADD_SUBCOMMAND, "Add a quote")
					.addOption(OptionType.STRING, QUOTEE_ARGUMENT, "Person to quote", true)
					.addOption(OptionType.STRING, MESSAGE_ARGUMENT, "Quote message", true),
				SubcommandData(GET_SUBCOMMAND, "Get a quote by ID")
					.addOption(OptionType.INTEGER, ID_ARGUMENT, "Quote id", true),
				SubcommandData(DELETE_SUBCOMMAND, "Delete a quote by ID")
					.addOption(OptionType.INTEGER, ID_ARGUMENT, "Quote id", true),
				SubcommandData(LIST_SUBCOMMAND, "List all quotes")
			)
	}

}