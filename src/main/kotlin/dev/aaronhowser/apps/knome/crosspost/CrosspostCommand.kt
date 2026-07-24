package dev.aaronhowser.apps.knome.crosspost

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

object CrosspostCommand {

	const val COMMAND_NAME = "crosspost"
	const val START_ARGUMENT = "start"
	const val END_ARGUMENT = "end"

	fun getCommand(): SlashCommandData {
		return Commands.slash(COMMAND_NAME, "Crosspost messages")
			.addOption(OptionType.STRING, START_ARGUMENT, "First message ID", true)
			.addOption(OptionType.STRING, END_ARGUMENT, "Last message ID", false)
			.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
	}

	suspend fun handleCrosspost(event: SlashCommandInteractionEvent) {
		event.deferReply(true).complete()

		val startId = event.getOption(START_ARGUMENT)?.asLong

		if (startId == null) {
			event.hook.sendMessage("Start id is required.").queue()
			return
		}

		val endId = event.getOption(END_ARGUMENT)?.asLong ?: startId
		val channel = event.channel

		try {
			CrosspostService.crosspost(
				jda = event.jda,
				startMessageId = startId,
				endMessageId = endId,
				channel = channel
			)

			event.hook.sendMessage("Done!").queue()
		} catch (e: IllegalArgumentException) {
			event.hook.sendMessage("Error: ${e.message}").queue()
		} catch (e: Exception) {
			event.hook.sendMessage("An unexpected error occurred: ${e.message}").queue()
		}
	}

}