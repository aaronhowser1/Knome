package org.example.dev.aaronhowser.apps.knome.feature

import com.mongodb.client.model.Aggregates
import com.mongodb.client.model.Filters
import com.mongodb.client.model.Sorts

typealias Quote = Pair<String, String> // Pair<Username, Message>
typealias QuoteWithId = Triple<Int, String, String> // Triple<Id, Username, Message>

object QuoteFeature {

	private const val ID_FIELD = "id"
	private const val USER_FIELD = "user"
	private const val MESSAGE_FIELD = "message"

	private fun getNextId(): Int {
		val lastDocument = QuoteRepository.quotes
			.find()
			.sort(Sorts.descending(ID_FIELD))
			.limit(1)
			.first()

		return if (lastDocument != null) {
			lastDocument.getInteger(ID_FIELD) + 1
		} else {
			1
		}
	}

	fun addQuote(user: String, message: String) {
		val newId = getNextId()
		val newQuote = org.bson.Document()
			.append(ID_FIELD, newId)
			.append(USER_FIELD, user)
			.append(MESSAGE_FIELD, message)

		QuoteRepository.quotes.insertOne(newQuote)
	}

	fun getRandomQuote(): Quote? {
		val document = QuoteRepository.quotes
			.aggregate(
				listOf(Aggregates.sample(1))
			).firstOrNull() ?: return null

		val user = document.getString(USER_FIELD)
		val message = document.getString(MESSAGE_FIELD)

		return Quote(user, message)
	}

	fun getQuote(id: Int): Quote? {
		val document = QuoteRepository.quotes
			.find(Filters.eq(ID_FIELD, id))
			.firstOrNull()
			?: return null

		val userName = document.getString(USER_FIELD)
		val message = document.getString(MESSAGE_FIELD)

		return Pair(userName, message)
	}

	fun removeQuote(id: Int): Quote? {
		val document = QuoteRepository.quotes
			.findOneAndDelete(Filters.eq(ID_FIELD, id))
			?: return null

		val userName = document.getString(USER_FIELD)
		val message = document.getString(MESSAGE_FIELD)

		return Pair(userName, message)
	}

	fun getQuotes(
		amount: Int = 10,
		startingAt: Int = 0
	): List<QuoteWithId> {
		val quotes = mutableListOf<QuoteWithId>()

		QuoteRepository.quotes.find()
			.sort(Sorts.ascending(ID_FIELD))
			.skip(startingAt)
			.limit(amount)
			.forEach { document ->
				val id = document.getInteger(ID_FIELD)
				val userName = document.getString(USER_FIELD)
				val message = document.getString(MESSAGE_FIELD)

				quotes.add(Triple(id, userName, message))
			}

		return quotes
	}

}