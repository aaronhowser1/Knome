package org.example.dev.aaronhowser.apps.knome.feature

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.bson.Document

object QuoteRepository {

	private val mongoClient: MongoClient = MongoClients.create("mongodb://localhost:27017")
	private val database: MongoDatabase = mongoClient.getDatabase("knome_bot")

	val quotes: MongoCollection<Document> = database.getCollection("quotes")

}