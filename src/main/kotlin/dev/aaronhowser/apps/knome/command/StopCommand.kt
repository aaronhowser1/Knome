package dev.aaronhowser.apps.knome.command

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import kotlin.system.exitProcess

object StopCommand {

	const val COMMAND_NAME = "stop"
	const val RESTART_AFTER_ARGUMENT = "restart_after"

	fun getCommand(): SlashCommandData {
		return Commands.slash(COMMAND_NAME, "Stop bot")
			.addOption(OptionType.BOOLEAN, RESTART_AFTER_ARGUMENT, "Restart after", false)
			.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR))
	}

	fun handleStop(event: SlashCommandInteractionEvent) {
		val shouldRestart = event.getOption(RESTART_AFTER_ARGUMENT)?.asBoolean ?: false

		event.deferReply(true).queue { hook ->
			hook.sendMessage("Stopping bot...${if (shouldRestart) " (will restart)" else ""}").queue {
				Thread.sleep(200)
				exitProcess(if (shouldRestart) 2 else 99) // 2 = restart, 99 = kill
			}
		}
	}

}