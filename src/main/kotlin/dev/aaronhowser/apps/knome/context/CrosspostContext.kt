package org.example.dev.aaronhowser.apps.knome.context

import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.interactions.commands.Command
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.Commands
import net.dv8tion.jda.api.interactions.commands.context.ContextInteraction

object CrosspostContext {

	const val COMMAND_NAME = "crosspost"

	fun getContext(): CommandData {
		return Commands.context(Command.Type.MESSAGE, COMMAND_NAME)
	}

	fun handleContextCrosspost(event: MessageContextInteractionEvent) {
		if (event.targetType != ContextInteraction.ContextTarget.MESSAGE) {
			event.hook.sendMessage("This context menu only works on messages.").queue()
			return
		}

		val startMessageId = event.target.idLong

		event.hook
			.sendMessage("Please provide the end message ID.")

	}

}