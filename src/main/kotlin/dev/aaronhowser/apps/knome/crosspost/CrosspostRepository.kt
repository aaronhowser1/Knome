package dev.aaronhowser.apps.knome.crosspost

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.ReplaceOptions
import dev.aaronhowser.apps.knome.quote.QuoteRepository
import org.bson.Document
import java.util.Date

object CrosspostRepository {

	private const val CHANNEL_ID_FIELD = "channel_id"
	private const val ID_FIELD = "_id"
	private const val MESSAGE_ID_FIELD = "message_id"
	private const val DESTINATION_FIELD = "destination"
	private const val URL_FIELD = "url"
	private const val PUBLISHED_AT_FIELD = "published_at"

	private val crossposts: MongoCollection<Document> =
		QuoteRepository.database.getCollection("crossposts")

	fun getHistorySummary(messageIds: List<Long>): String {
		if (messageIds.isEmpty()) {
			return "No Discord messages selected."
		}

		val destinationCounts = mutableMapOf<String, Int>()
		val documents = crossposts.find(
			Filters.`in`(MESSAGE_ID_FIELD, messageIds.map { messageId -> messageId.toString() })
		)
		for (document in documents) {
			val destination = document.getString(DESTINATION_FIELD)
			destinationCounts[destination] = destinationCounts.getOrDefault(destination, 0) + 1
		}

		if (destinationCounts.isEmpty()) {
			return "None of the ${messageIds.size} selected message(s) have been crossposted."
		}

		return listOf("Tumblr", "Bluesky").joinToString("\n") { destination ->
			val count = destinationCounts.getOrDefault(destination, 0)
			"$destination: $count/${messageIds.size} selected message(s)"
		}
	}

	fun recordSuccessfulPublications(draft: CrosspostDraft, results: List<CrosspostResult>) {
		for (result in results) {
			if (!result.succeeded || result.url == null) {
				continue
			}

			for (messageId in draft.messageIds) {
				val recordId = "$messageId:${result.destination.lowercase()}"
				val filter = Filters.eq(ID_FIELD, recordId)
				val document = Document()
					.append(ID_FIELD, recordId)
					.append(CHANNEL_ID_FIELD, draft.channelId.toString())
					.append(MESSAGE_ID_FIELD, messageId.toString())
					.append(DESTINATION_FIELD, result.destination)
					.append(URL_FIELD, result.url)
					.append(PUBLISHED_AT_FIELD, Date())

				crossposts.replaceOne(filter, document, ReplaceOptions().upsert(true))
			}
		}
	}
}