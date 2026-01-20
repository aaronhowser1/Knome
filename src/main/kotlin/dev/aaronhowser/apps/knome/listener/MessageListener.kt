package dev.aaronhowser.apps.knome.listener

import dev.aaronhowser.apps.knome.util.AaronServerConstants
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import kotlin.random.Random

class MessageListener : ListenerAdapter() {

	override fun onMessageReceived(event: MessageReceivedEvent) {
		val author = event.author
		val message = event.message
		val channel = event.channel
		val guild = event.guild

		if (author.isBot) return

		if (message.mentions.isMentioned(event.jda.selfUser)) {
			message.reply("!!!").queue()
		}

		if (author.idLong == AaronServerConstants.ARIEL_MEMBER_ID && Random.nextInt(100) == 0) {
			val insults = listOf("wrong", "stfu", "shut up", "no", "and?", "whatever")
			message.reply(insults.random()).queue()
			return
		}

		if (author.idLong == AaronServerConstants.AARON_MEMBER_ID) {
			if (channel.idLong == AaronServerConstants.PHILOSOPHY_CHANNEL_ID) return

			val ignoredGroups = listOf(
				AaronServerConstants.MOD_UPDATES_GROUP_ID,
				AaronServerConstants.SERVER_GROUP_ID
			)

			if (ignoredGroups.any { groupId -> AaronServerConstants.channelIsInGroup(guild, channel.idLong, groupId) }) return

			if (Random.nextInt(200) == 0) {
				val affirmations = listOf("so true", "real", "facts")
				message.reply(affirmations.random()).queue()
			}
		}

		if (channel.idLong == AaronServerConstants.OFF_TOPIC_CHANNEL_ID) {
			if (Random.nextInt(300) == 0) {
				event.channel.sendTyping()
			}
		}
	}

}