package dev.aaronhowser.apps.knome.crosspost

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion

object CrosspostService {

	suspend fun crosspost(
		jda: JDA,
		startMessageId: Long,
		endMessageId: Long = startMessageId,
		channel: MessageChannelUnion
	) {
		val messages = DiscordMessageRangeReader.read(
			channel = channel,
			startMessageId = startMessageId,
			endMessageId = endMessageId
		)

		val authorId = messages.first().author.idLong
		require(messages.all { message -> message.author.idLong == authorId }) {
			"All messages must be from the same author."
		}

		val content = messages
			.joinToString("\n") { message -> message.contentDisplay }
			.trim()

		CrosspostAuditLog.publish(jda, content)
	}

}