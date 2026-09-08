package dev.aaronhowser.apps.knome.crosspost.command

import dev.aaronhowser.apps.knome.crosspost.model.*

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

object CrosspostSeriesCommand {

	const val COMMAND_NAME = CrosspostCommand.SERIES_COMMAND_NAME

	fun getCommand() = CrosspostCommand.getSeriesCommand()

	suspend fun handle(event: SlashCommandInteractionEvent) {
		CrosspostCommand.handleSeries(event)
	}
}