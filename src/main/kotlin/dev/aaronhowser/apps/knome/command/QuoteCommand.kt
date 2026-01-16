package org.example.dev.aaronhowser.apps.knome.command

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import org.example.dev.aaronhowser.apps.knome.feature.Quote
import org.example.dev.aaronhowser.apps.knome.feature.QuoteFeature

object QuoteCommand {

	const val COMMAND_NAME = "quote"

	const val ADD_SUBCOMMAND = "add"
	const val QUOTEE_ARGUMENT = "quotee"
	const val MESSAGE_ARGUMENT = "message"

	const val GET_SUBCOMMAND = "get"
	const val ID_ARGUMENT = "id"

	const val LIST_SUBCOMMAND = "list"
	const val AMOUNT_SUBCOMMAND = "amount"
	const val STARTING_AT_ARGUMENT = "starting_at"

	const val DELETE_SUBCOMMAND = "delete"

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
				SubcommandData(LIST_SUBCOMMAND, "List quotes")
					.addOption(OptionType.INTEGER, AMOUNT_SUBCOMMAND, "Number of quotes to list", false)
					.addOption(OptionType.INTEGER, STARTING_AT_ARGUMENT, "Starting quote ID", false)
			)
	}

	suspend fun handleQuote(event: SlashCommandInteractionEvent) {
		event.deferReply(false).complete()

		val subcommand = event.subcommandName

		when (subcommand) {
			ADD_SUBCOMMAND -> handleAddQuote(event)
			GET_SUBCOMMAND -> handleGetQuote(event)
			DELETE_SUBCOMMAND -> handleDeleteQuote(event)
			LIST_SUBCOMMAND -> handleListQuotes(event)
		}
	}

	private fun handleAddQuote(event: SlashCommandInteractionEvent) {
		val quotee = event.getOption(QUOTEE_ARGUMENT)?.asString
		val message = event.getOption(MESSAGE_ARGUMENT)?.asString

		if (quotee == null || message == null) {
			event.hook.sendMessage("Quotee and message are required.").queue()
			return
		}

		val quote = QuoteFeature.addQuote(
			user = quotee,
			message = message
		)

		event.hook.sendMessage("Added quote #${quote.id} by ${quote.user}: \"${quote.message}\"").queue()
	}

	private fun handleGetQuote(event: SlashCommandInteractionEvent) {
		val id = event.getOption(ID_ARGUMENT)?.asInt

		if (id == null) {
			event.hook.sendMessage("Quote ID is required.").queue()
			return
		}

		val quote = QuoteFeature.getQuote(id)
		if (quote == null) {
			event.hook.sendMessage("Quote with ID $id not found.").queue()
			return
		}

		event.hook.sendMessageEmbeds(quote.getEmbed()).queue()
	}

	private fun handleDeleteQuote(event: SlashCommandInteractionEvent) {
		val id = event.getOption(ID_ARGUMENT)?.asInt

		if (id == null) {
			event.hook.sendMessage("Quote ID is required.").queue()
			return
		}

		val deletedQuote = QuoteFeature.removeQuote(id)
		if (deletedQuote == null) {
			event.hook.sendMessage("Quote with ID $id not found.").queue()
			return
		}

		event.hook.sendMessage("Deleted quote #$id by ${deletedQuote.user}: \"${deletedQuote.message}\"").queue()
	}

	private fun handleListQuotes(event: SlashCommandInteractionEvent) {
		val amount = event.getOption(AMOUNT_SUBCOMMAND)?.asInt ?: 10
		val startingAt = event.getOption(STARTING_AT_ARGUMENT)?.asInt ?: 0

		val quotes = QuoteFeature.getQuotes(amount, startingAt)

		if (quotes.isEmpty()) {
			event.hook.sendMessage("No quotes found.").queue()
			return
		}

		event.hook.sendMessageEmbeds(quotes.map(Quote::getEmbed)).queue()
	}

}