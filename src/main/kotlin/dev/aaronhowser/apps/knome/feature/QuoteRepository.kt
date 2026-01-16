package org.example.dev.aaronhowser.apps.knome.feature

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.bson.UuidRepresentation

object QuoteRepository {

	private val mongoClient: MongoClient = MongoClients.create(
		MongoClientSettings.builder()
			.applyConnectionString(ConnectionString("mongodb://127.0.0.1:27017/?directConnection=true&serverSelectionTimeoutMS=2000"))
			.applicationName("knome")
			.uuidRepresentation(UuidRepresentation.STANDARD)
			.build()
	)

	private val database: MongoDatabase = mongoClient.getDatabase("knome_bot")

	val quotes: MongoCollection<Document> = database.getCollection("quotes")

}

data class Quote(val user: String, val message: String)
data class QuoteWithId(val id: Int, val user: String, val message: String)