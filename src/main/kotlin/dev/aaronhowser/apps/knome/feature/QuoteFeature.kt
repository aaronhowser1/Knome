package org.example.dev.aaronhowser.apps.knome.feature

import java.sql.Connection
import java.sql.DriverManager
import kotlin.random.Random

typealias Quote = Pair<String, String> // Pair<Username, Message>
typealias QuoteWithId = Triple<Int, String, String> // Triple<Id, Username, Message>

object QuoteFeature {

	private val connection: Connection = DriverManager.getConnection("jdbc:sqlite:quotes.db")

	private const val TABLE_NAME = "quotes"
	private const val ID_COLUMN = "id"
	private const val USERNAME_COLUMN = "user"
	private const val MESSAGE_COLUMN = "message"

	init {
		connection.createStatement().use { statement ->
			statement.executeUpdate(
				StringBuilder()
					.append("CREATE TABLE IF NOT EXISTS $TABLE_NAME (")
					.append("$ID_COLUMN INTEGER PRIMARY KEY AUTOINCREMENT,")
					.append("$USERNAME_COLUMN TEXT NOT NULL")
					.append("$MESSAGE_COLUMN TEXT NOT NULL")
					.append(");")
					.toString()
			)
		}
	}

	fun addQuote(userName: String, message: String) {
		connection.prepareStatement(
			"INSERT INTO $TABLE_NAME ($USERNAME_COLUMN, $MESSAGE_COLUMN) VALUES (?, ?);"
		).use { statement ->
			statement.setString(1, userName)
			statement.setString(2, message)
			statement.executeUpdate()
		}
	}

	fun getRandomQuote(): Quote? {
		val quoteCount = connection.prepareStatement(
			"SELECT COUNT(*) FROM $TABLE_NAME"
		).use { countStatement ->
			countStatement.executeQuery().use { resultSet ->
				if (resultSet.next()) {
					resultSet.getInt(1)
				} else {
					0
				}
			}
		}

		if (quoteCount == 0) return null

		return connection.prepareStatement("SELECT $USERNAME_COLUMN, $MESSAGE_COLUMN FROM $TABLE_NAME LIMIT 1 OFFSET ?").use { statement ->
			statement.setInt(1, Random.nextInt(quoteCount))

			statement.executeQuery().use { resultSet ->
				if (resultSet.next()) {
					val userName = resultSet.getString(USERNAME_COLUMN)
					val message = resultSet.getString(MESSAGE_COLUMN)
					Pair(userName, message)
				} else {
					null
				}
			}
		}
	}

}