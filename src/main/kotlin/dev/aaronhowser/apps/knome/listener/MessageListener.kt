package dev.aaronhowser.apps.knome.listener

import dev.aaronhowser.apps.knome.discord.AaronServer
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
			message.reply(getRandomInsult()).queue()
		}

		if (author.idLong == AaronServer.ARIEL_MEMBER_ID) {
			val shouldInsult = Random.nextInt(100) == 0
					|| message.contentRaw.lowercase().contains("be nice")

			if (shouldInsult) {
				message.reply(getRandomInsult()).queue()
				return
			}
		}

		if (author.idLong == AaronServer.AARON_MEMBER_ID) {
			if (channel.idLong == AaronServer.PHILOSOPHY_CHANNEL_ID) return

			val ignoredGroups = listOf(
				AaronServer.MOD_UPDATES_GROUP_ID,
				AaronServer.SERVER_GROUP_ID
			)

			if (ignoredGroups.any { groupId -> AaronServer.channelIsInGroup(guild, channel.idLong, groupId) }) return

			if (Random.nextInt(200) == 0) {
				val affirmations = listOf("so true", "real", "facts")
				message.reply(affirmations.random()).queue()
			}
		}

		if (channel.idLong == AaronServer.OFF_TOPIC_CHANNEL_ID) {
			if (Random.nextInt(300) == 0) {
				event.channel.sendTyping()
			}
		}
	}

	fun getRandomInsult(): String {
		val insults = listOf(
			"wrong",
			"stfu",
			"shut up",
			"no",
			"and?",
			"whatever",
			"go away",
			"ugh"
		)

		return insults.random()
	}
}