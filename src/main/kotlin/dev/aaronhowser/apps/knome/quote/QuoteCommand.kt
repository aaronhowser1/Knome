package dev.aaronhowser.apps.knome.quote

import dev.aaronhowser.apps.knome.KnomeBot
import dev.aaronhowser.apps.knome.discord.await
import dev.aaronhowser.apps.knome.quote.Quote.Companion.getEmbedDescription
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData

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
	const val GET_RANDOM_SUBCOMMAND = "random"

	fun getCommand(): SlashCommandData {
		return Commands.slash(COMMAND_NAME, "Quotes!")
			.addSubcommands(
				SubcommandData(ADD_SUBCOMMAND, "Add a quote")
					.addOptions(
						OptionData(OptionType.STRING, QUOTEE_ARGUMENT, "Person to quote", true)
							.setRequiredLength(1, MAX_QUOTEE_LENGTH),
						OptionData(OptionType.STRING, MESSAGE_ARGUMENT, "Quote message", true)
							.setRequiredLength(1, MAX_MESSAGE_LENGTH)
					),

				SubcommandData(GET_SUBCOMMAND, "Get a quote by ID")
					.addOptions(idOption()),

				SubcommandData(DELETE_SUBCOMMAND, "Delete a quote by ID")
					.addOptions(idOption()),

				SubcommandData(LIST_SUBCOMMAND, "List quotes")
					.addOptions(
						OptionData(OptionType.INTEGER, AMOUNT_SUBCOMMAND, "Number of quotes to list")
							.setRequiredRange(1, MAX_LIST_AMOUNT.toLong()),
						OptionData(OptionType.INTEGER, STARTING_AT_ARGUMENT, "Starting quote ID")
							.setMinValue(0)
					),

				SubcommandData(GET_RANDOM_SUBCOMMAND, "Get a random quote")
			)
	}

	suspend fun handleQuote(event: SlashCommandInteractionEvent) {
		event.deferReply().await()

		try {
			when (val subcommand = event.subcommandName) {
				ADD_SUBCOMMAND -> handleAddQuote(event)
				GET_SUBCOMMAND -> handleGetQuote(event)
				DELETE_SUBCOMMAND -> handleDeleteQuote(event)
				LIST_SUBCOMMAND -> handleListQuotes(event)
				GET_RANDOM_SUBCOMMAND -> handleGetRandomQuote(event)
				else -> event.hook.sendMessage("Unknown subcommand: $subcommand").queue()
			}
		} catch (exception: Exception) {
			KnomeBot.LOGGER.severe("Quote command failed: ${exception.stackTraceToString()}")
			event.hook.sendMessage("The quote database is unavailable. Please try again later.").queue()
		}
	}

	private fun handleAddQuote(event: SlashCommandInteractionEvent) {
		val quotee = event.getOption(QUOTEE_ARGUMENT)?.asString
		val message = event.getOption(MESSAGE_ARGUMENT)?.asString

		if (quotee == null || message == null) {
			event.hook.sendMessage("Quotee and message are required.").queue()
			return
		}

		val quote = QuoteService.addQuote(
			user = quotee,
			message = message
		)

		val embed = EmbedBuilder()
			.setTitle("Added Quote #${quote.id}")
			.setDescription(quote.message)
			.setFooter("- ${quote.user}")
			.build()

		event.hook.sendMessageEmbeds(embed).queue()
	}

	private fun handleGetQuote(event: SlashCommandInteractionEvent) {
		val id = event.getOption(ID_ARGUMENT)?.asInt

		if (id == null) {
			event.hook.sendMessage("Quote ID is required.").queue()
			return
		}

		val quote = QuoteService.getQuote(id)
		if (quote == null) {
			event.hook.sendMessage("Quote with ID $id not found.").queue()
			return
		}

		event.hook.sendMessageEmbeds(quote.getEmbed()).queue()
	}

	private fun handleGetRandomQuote(event: SlashCommandInteractionEvent) {
		val quote = QuoteService.getRandomQuote()
		if (quote == null) {
			event.hook.sendMessage("No quotes found.").queue()
			return
		}

		event.hook.sendMessageEmbeds(quote.getEmbed()).queue()
	}

	private fun handleDeleteQuote(event: SlashCommandInteractionEvent) {
		if (event.member?.hasPermission(Permission.ADMINISTRATOR) != true) {
			event.hook.sendMessage("You need the Administrator permission to delete quotes.").queue()
			return
		}

		val id = event.getOption(ID_ARGUMENT)?.asInt

		if (id == null) {
			event.hook.sendMessage("Quote ID is required.").queue()
			return
		}

		val deletedQuote = QuoteService.removeQuote(id)
		if (deletedQuote == null) {
			event.hook.sendMessage("Quote with ID $id not found.").queue()
			return
		}

		val embed = EmbedBuilder()
			.setTitle("Deleted Quote #$id")
			.setDescription(deletedQuote.message)
			.setFooter("- ${deletedQuote.user}")
			.build()

		event.hook.sendMessageEmbeds(embed).queue()
	}

	private fun handleListQuotes(event: SlashCommandInteractionEvent) {
		val amount = event.getOption(AMOUNT_SUBCOMMAND)?.asInt ?: 10
		val startingAt = event.getOption(STARTING_AT_ARGUMENT)?.asInt ?: 0

		val quotes = QuoteService.getQuotes(amount, startingAt)

		if (quotes.isEmpty()) {
			event.hook.sendMessage("No quotes found.").queue()
			return
		}

		val amountQuotes = QuoteService.getMaxId() ?: 0

		val embed = EmbedBuilder()
			.setTitle("Quotes ${quotes.first().id} - ${quotes.last().id} / $amountQuotes")
			.setDescription(quotes.getEmbedDescription())
			.build()

		event.hook.sendMessageEmbeds(embed).queue()
	}

	private fun idOption(): OptionData {
		return OptionData(OptionType.INTEGER, ID_ARGUMENT, "Quote ID", true)
			.setMinValue(0)
	}

	private const val MAX_QUOTEE_LENGTH = 100
	private const val MAX_MESSAGE_LENGTH = 1_000
	private const val MAX_LIST_AMOUNT = 25
}
