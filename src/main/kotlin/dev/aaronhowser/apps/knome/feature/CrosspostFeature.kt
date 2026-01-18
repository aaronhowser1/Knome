package dev.aaronhowser.apps.knome.feature

import dev.aaronhowser.apps.knome.EnvironmentKeys
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import dev.aaronhowser.apps.knome.util.AaronServerConstants
import dev.aaronhowser.apps.knome.util.DiscordUtils

object CrosspostFeature {

	suspend fun crosspost(
		jda: JDA,
		startId: Long,
		endId: Long = startId,
		channel: MessageChannelUnion
	) {
		val messages = DiscordUtils.fetchMessagesBetween(
			channel,
			startId,
			endId
		)

		val firstAuthorId = messages.first().author.idLong
		require(messages.all { it.author.idLong == firstAuthorId }) { "All messages must be from the same author." }

		val combinedText = messages
			.joinToString("\n") { it.contentDisplay }
			.trim()

		postToModlog(jda, combinedText)
	}

	private fun postToModlog(jda: JDA, content: String) {
		val modLogChannel = AaronServerConstants.getModlog(jda)

		val embed = EmbedBuilder()
			.setTitle("Cross-post")
			.setColor(0x7289DA)
			.setDescription("Messages reposted from Discord:\n\n$content")
			.addField("Tumblr", "todo", true)
			.addField("Bluesky", "todo", true)
			.setFooter("Knome Bot")
			.build()

		modLogChannel
			.sendMessageEmbeds(embed)
			.queue()
	}

	private suspend fun postToTumblr(content: String) {
		val client = HttpClient(CIO) {
			install(ContentNegotiation) {
				json()
			}
		}

		val accessToken = "test"

		val apiLocation = "https://api.tumblr.com/v2/blog/${EnvironmentKeys.TUMBLR_ID}/post"

		val body = content
			.trim()
			.replace("\"", "\\\"")
			.let { "\"$it\"" }

		val response = client.post(apiLocation) {
			header(HttpHeaders.Authorization, "Bearer $accessToken")
			contentType(ContentType.Application.Json)
			setBody(
				"""{
            "type": "text",
            "body": $body
        }"""
			)
		}

		client.close()

		if (response.status.value !in 200..299) {
			throw Exception("Failed to post to Tumblr: ${response.status}")
		}
	}

}