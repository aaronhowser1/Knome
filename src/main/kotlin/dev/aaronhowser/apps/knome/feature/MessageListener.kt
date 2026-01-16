package org.example.dev.aaronhowser.apps.knome.feature

import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter

class MessageListener : ListenerAdapter() {

	override fun onMessageReceived(event: MessageReceivedEvent) {
		if (event.author.isBot) return

		if (event.message.mentions.isMentioned(event.jda.selfUser)) {
			event.channel.sendMessage("!!!").queue()
		}
	}

}