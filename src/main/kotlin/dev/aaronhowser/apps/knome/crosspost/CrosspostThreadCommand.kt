package dev.aaronhowser.apps.knome.crosspost

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent

object CrosspostThreadCommand {

	const val COMMAND_NAME = CrosspostCommand.THREAD_COMMAND_NAME

	fun getCommand() = CrosspostCommand.getThreadCommand()

	suspend fun handle(event: SlashCommandInteractionEvent) {
		CrosspostCommand.handleThread(event)
	}
}