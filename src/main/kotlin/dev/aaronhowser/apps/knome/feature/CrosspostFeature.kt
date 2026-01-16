package org.example.dev.aaronhowser.apps.knome.feature

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import org.example.dev.aaronhowser.apps.knome.util.AaronServerConstants
import org.example.dev.aaronhowser.apps.knome.util.DiscordUtils

object CrosspostFeature {

	suspend fun crosspost(
		jda: JDA,
		startId: Long,
		endId: Long = startId,
		channel: MessageChannelUnion
	) {
		val messages = DiscordUtils.fetchMessagesBetween(
			channel,
			startId,
			endId
		)

		val firstAuthorId = messages.first().author.idLong
		require(messages.all { it.author.idLong == firstAuthorId }) { "All messages must be from the same author." }

		val combinedText = messages
			.joinToString("\n") { it.contentDisplay }
			.trim()

		postToModlog(jda, combinedText)
	}

	private fun postToModlog(jda: JDA, content: String) {
		val modLogChannel = AaronServerConstants.getModlog(jda)

		val embed = EmbedBuilder()
			.setTitle("Cross-post")
			.setColor(0x7289DA)
			.setDescription("Messages reposted from Discord:\n\n$content")
			.addField("Tumblr", "todo", true)
			.addField("Bluesky", "todo", true)
			.setFooter("Knome Bot")
			.build()

		modLogChannel
			.sendMessageEmbeds(embed)
			.queue()
	}
}