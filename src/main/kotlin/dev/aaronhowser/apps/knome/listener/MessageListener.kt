package dev.aaronhowser.apps.knome.listener

import dev.aaronhowser.apps.knome.util.AaronServerConstants
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import kotlin.random.Random

class MessageListener : ListenerAdapter() {

	override fun onMessageReceived(event: MessageReceivedEvent) {
		if (event.author.isBot) return

		if (event.message.mentions.isMentioned(event.jda.selfUser)) {
			event.message.reply("!!!").queue()
		}

		if (event.author.idLong == AaronServerConstants.ARIEL_MEMBER_ID) {
			if (Random.nextInt(100) == 0) {
				val insults = listOf(
					"wrong", "stfu", "shut up", "no", "and?", "whatever"
				)
				event.message.reply(insults.random()).queue()
			}
		}

		if (event.author.idLong == AaronServerConstants.AARON_MEMBER_ID) {

			if (event.channel.idLong == AaronServerConstants.PHILOSOPHY_CHANNEL_ID) return
			if (AaronServerConstants.channelIsInGroup(event.guild, event.channel.idLong, AaronServerConstants.MOD_UPDATES_GROUP_ID)) return
			if (AaronServerConstants.channelIsInGroup(event.guild, event.channel.idLong, AaronServerConstants.SERVER_GROUP_ID)) return

			if (Random.nextInt(200) == 0) {
				val affirmations = listOf(
					"so true", "real", "facts"
				)

				event.message.reply(affirmations.random()).queue()
			}
		}
	}

}