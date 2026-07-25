package dev.aaronhowser.apps.knome.quote

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bson.Document
import org.bson.UuidRepresentation

object QuoteRepository {

	private val mongoClient: MongoClient = MongoClients.create(
		MongoClientSettings.builder()
			.applyConnectionString(ConnectionString(getConnectionString()))
			.applicationName("knome")
			.uuidRepresentation(UuidRepresentation.STANDARD)
			.build()
	)

	internal val database: MongoDatabase = mongoClient.getDatabase("knome_bot")

	val quotes: MongoCollection<Document> = database.getCollection("quotes")

	suspend fun isOnline(): Boolean {
		return withContext(Dispatchers.IO) {
			try {
				database.runCommand(Document("ping", 1))
				true
			} catch (e: Exception) {
				false
			}
		}
	}

	fun close() {
		mongoClient.close()
	}

	private fun getConnectionString(): String {
		return System.getenv("KNOME_MONGO_URI")
			?: "mongodb://127.0.0.1:27017/?directConnection=true&serverSelectionTimeoutMS=2000"
	}
}