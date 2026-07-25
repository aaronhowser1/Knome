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

	fun getHistorySummary(draft: CrosspostDraft): String {
		if (draft.messageIds.isEmpty()) {
			return "No Discord messages selected."
		}

		val destinationsByMessageId = mutableMapOf<Long, MutableList<String>>()
		val documents = crossposts.find(
			Filters.`in`(MESSAGE_ID_FIELD, draft.messageIds.map { messageId -> messageId.toString() })
		)
		for (document in documents) {
			val messageId = document.getString(MESSAGE_ID_FIELD).toLong()
			val destination = document.getString(DESTINATION_FIELD)
			destinationsByMessageId.getOrPut(messageId) { mutableListOf() }.add(destination)
		}

		if (destinationsByMessageId.isEmpty()) {
			return "None of the ${draft.messageIds.size} selected message(s) have been crossposted."
		}

		val lines = mutableListOf<String>()
		for (index in draft.messageIds.indices) {
			val messageId = draft.messageIds[index]
			val destinations = destinationsByMessageId[messageId] ?: continue
			lines.add("[Message ${index + 1}](${draft.messageLinks[index]}): ${destinations.sorted().joinToString(", ")}")
		}

		val unpostedCount = draft.messageIds.size - destinationsByMessageId.size
		if (unpostedCount > 0) {
			lines.add("$unpostedCount selected message(s) have not been crossposted.")
		}

		val visibleLines = mutableListOf<String>()
		for (line in lines) {
			if ((visibleLines + line).joinToString("\n").length > 900) {
				break
			}
			visibleLines.add(line)
		}
		val omittedCount = lines.size - visibleLines.size
		if (omittedCount > 0) {
			visibleLines.add("…and $omittedCount more.")
		}
		return visibleLines.joinToString("\n")
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