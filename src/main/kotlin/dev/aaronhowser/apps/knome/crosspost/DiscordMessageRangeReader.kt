package dev.aaronhowser.apps.knome.crosspost

import dev.aaronhowser.apps.knome.discord.await
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel

object DiscordMessageRangeReader {

	suspend fun read(
		channel: MessageChannel,
		startMessageId: Long,
		endMessageId: Long
	): List<Message> {
		return withContext(Dispatchers.IO) {
			val minId = minOf(startMessageId, endMessageId)
			val maxId = maxOf(startMessageId, endMessageId)
			val collectedMessages = mutableListOf<Message>()
			var lastMessageId = maxId

			val endMessage = channel.retrieveMessageById(maxId).await()
			channel.retrieveMessageById(minId).await()
			collectedMessages.add(endMessage)

			if (minId == maxId) {
				return@withContext collectedMessages
			}

			while (collectedMessages.size <= MAX_MESSAGES) {
				val batch = channel.getHistoryBefore(lastMessageId, 100).await().retrievedHistory
				require(batch.isNotEmpty()) { "The selected message range could not be read." }

				for (message in batch) {
					if (message.idLong < minId) {
						return@withContext collectedMessages.reversed()
					}

					collectedMessages.add(message)
					if (message.idLong == minId) {
						return@withContext collectedMessages.reversed()
					}
				}

				lastMessageId = batch.last().idLong
			}

			throw IllegalArgumentException("A crosspost can include at most $MAX_MESSAGES Discord messages.")
		}
	}

	private const val MAX_MESSAGES = 50
}