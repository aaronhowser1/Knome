package org.example.dev.aaronhowser.apps.knome.context

import net.dv8tion.jda.api.events.interaction.command.MessageContextInteractionEvent
import net.dv8tion.jda.api.events.session.ReadyEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class ContextListener : ListenerAdapter() {

	override fun onMessageContextInteraction(event: MessageContextInteractionEvent) {
		when (event.name) {
			CrosspostContext.COMMAND_NAME -> {
				event.reply("test")
			}
		}
	}

	override fun onReady(event: ReadyEvent) {
		event.jda.updateCommands()
			.addCommands(CrosspostContext.getContext())
			.queue()
	}

}