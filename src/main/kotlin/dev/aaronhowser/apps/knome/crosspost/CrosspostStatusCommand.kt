package dev.aaronhowser.apps.knome.crosspost

import dev.aaronhowser.apps.knome.discord.AaronServer
import dev.aaronhowser.apps.knome.discord.await
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData

object CrosspostStatusCommand {

	const val COMMAND_NAME = "crosspost-status"

	fun getCommand(): SlashCommandData {
		return Commands.slash(COMMAND_NAME, "Check crosspost environment configuration")
			.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
	}

	suspend fun handle(event: SlashCommandInteractionEvent) {
		if (event.user.idLong != AaronServer.AARON_MEMBER_ID) {
			event.reply("Only Aaron can use crosspost commands.").setEphemeral(true).await()
			return
		}

		event.reply(CrosspostConfiguration.statusMessage()).setEphemeral(true).await()
	}
}