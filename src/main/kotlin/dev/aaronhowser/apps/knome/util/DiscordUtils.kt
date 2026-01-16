package dev.aaronhowser.apps.knome.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel
import dev.aaronhowser.apps.knome.util.ExtensionFunctions.await

object DiscordUtils {

	suspend fun fetchMessagesBetween(
		channel: MessageChannel,
		startMessageId: Long,
		endMessageId: Long
	): List<Message> {
		return withContext(Dispatchers.IO) {
			val minId = minOf(startMessageId, endMessageId)
			val maxId = maxOf(startMessageId, endMessageId)

			val collectedMessages = mutableListOf<Message>()
			var lastMessageId = maxId

			val lastMessage = channel
				.retrieveMessageById(maxId)
				.await()

			collectedMessages.add(lastMessage)

			while (true) {
				val batch = channel
					.getHistoryBefore(lastMessageId, 100)
					.await()
					.retrievedHistory

				if (batch.isEmpty()) break

				for (msg in batch) {
					if (msg.idLong < minId) {
						return@withContext collectedMessages.reversed()
					}

					collectedMessages.add(msg)
				}

				lastMessageId = batch.last().idLong
			}

			return@withContext collectedMessages.reversed()
		}
	}

}