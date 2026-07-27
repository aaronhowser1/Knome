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

		if (author.isBot) return

		val content = message.contentRaw.lowercase()
		val isMentioned = message.mentions.isMentioned(event.jda.selfUser)
		val isThanked = content.contains("thank you knome")
				|| isMentioned && content.contains("thank you")

		if (isThanked) {
			message.reply(YOUR_WELCOMES.random()).queue()
			return
		}

		if (isMentioned) {
			message.reply(getRandomInsult()).queue()
			return
		}

		if (author.idLong == AaronServer.ARIEL_MEMBER_ID) {
			val shouldInsult = Random.nextInt(100) == 0

			if (shouldInsult) {
				message.reply(getRandomInsult()).queue()
				return
			}
		}

		if (author.idLong == AaronServer.AARON_MEMBER_ID) {
			if (!event.isFromGuild) return
			if (channel.idLong == AaronServer.PHILOSOPHY_CHANNEL_ID) return

			val ignored = IGNORED_GROUP_IDS.any { groupId ->
				AaronServer.channelIsInGroup(event.guild, channel.idLong, groupId)
			}

			if (ignored) return

			if (Random.nextInt(200) == 0) {
				message.reply(AFFIRMATIONS.random()).queue()
			}
		}

		if (channel.idLong == AaronServer.OFF_TOPIC_CHANNEL_ID) {
			if (Random.nextInt(300) == 0) {
				event.channel.sendTyping().queue()
			}
		}
	}

	private fun getRandomInsult(): String = INSULTS.random()

	private companion object {
		val IGNORED_GROUP_IDS = setOf(
			AaronServer.MOD_UPDATES_GROUP_ID,
			AaronServer.SERVER_GROUP_ID
		)

		val AFFIRMATIONS = listOf("so true", "real", "facts")
		val YOUR_WELCOMES = listOf("de nada", "you're welcome", "your welcome")
		val INSULTS = listOf(
			"wrong",
			"stfu",
			"shut up",
			"no",
			"and?",
			"whatever",
			"go away",
			"ugh"
		)
	}
}