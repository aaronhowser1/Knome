package dev.aaronhowser.apps.knome.crosspost

import dev.aaronhowser.apps.knome.discord.AaronServer
import kotlinx.coroutines.*
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*
import java.util.concurrent.ConcurrentHashMap

object CrosspostService {

	private val drafts = ConcurrentHashMap<String, CrosspostDraft>()
	private val httpClient = HttpClient.newHttpClient()

	suspend fun prepare(
		ownerId: Long,
		startMessageId: Long,
		endMessageId: Long,
		channel: MessageChannelUnion
	): CrosspostDraft {
		require(ownerId == AaronServer.AARON_MEMBER_ID) {
			"Only Aaron can create crossposts."
		}

		require(channel.idLong == AaronServer.PHILOSOPHY_CHANNEL_ID) {
			"Crossposts can only be created from #philosophy."
		}

		val messages = DiscordMessageRangeReader.read(channel, startMessageId, endMessageId)
		require(messages.isNotEmpty()) { "No messages were found." }
		require(messages.all { message -> message.author.idLong == AaronServer.AARON_MEMBER_ID }) {
			"Every selected message must be from Aaron."
		}

		val messageText = messages.map { message -> message.contentRaw.trim() }
		require(messageText.any { content -> content.isNotBlank() } || messages.any { message -> message.attachments.isNotEmpty() }) {
			"The selected messages have no text or attachments."
		}

		val draft = CrosspostDraft(
			id = UUID.randomUUID().toString(),
			ownerId = ownerId,
			messages = messageText.filter { content -> content.isNotBlank() },
			images = downloadImages(messages),
			createdAtMillis = System.currentTimeMillis()
		)

		removeExpiredDrafts()
		drafts[draft.id] = draft
		return draft
	}

	fun getDraft(id: String, ownerId: Long): CrosspostDraft? {
		val draft = drafts[id] ?: return null
		if (draft.ownerId != ownerId || isExpired(draft)) {
			drafts.remove(id)
			return null
		}
		return draft
	}

	fun discardDraft(id: String, ownerId: Long): Boolean {
		val draft = getDraft(id, ownerId) ?: return false
		return drafts.remove(id, draft)
	}

	fun claimDraft(id: String, ownerId: Long): CrosspostDraft? {
		val draft = getDraft(id, ownerId) ?: return null
		return if (drafts.remove(id, draft)) draft else null
	}

	suspend fun publish(draft: CrosspostDraft, destination: CrosspostDestination): List<CrosspostResult> {
		return coroutineScope {
			val publishers = mutableListOf<suspend () -> CrosspostResult>()
			if (destination == CrosspostDestination.TUMBLR || destination == CrosspostDestination.BOTH) {
				publishers.add { TumblrPublisher.publish(draft) }
			}
			if (destination == CrosspostDestination.BLUESKY || destination == CrosspostDestination.BOTH) {
				publishers.add { BlueskyPublisher.publish(draft) }
			}
			publishers.map { publisher -> async { publisher() } }.awaitAll()
		}
	}

	private suspend fun downloadImages(messages: List<Message>): List<CrosspostImage> {
		val attachments = messages.flatMap { message -> message.attachments }
			.filter { attachment -> attachment.isImage }

		require(attachments.size <= MAX_IMAGES) {
			"A crosspost can include at most $MAX_IMAGES images."
		}

		require(attachments.sumOf { attachment -> attachment.size.toLong() } <= MAX_TOTAL_IMAGE_BYTES) {
			"Crosspost images can total at most 20 MB."
		}

		return withContext(Dispatchers.IO) {
			val images = mutableListOf<CrosspostImage>()
			for (attachment in attachments) {
				val request = HttpRequest.newBuilder(URI.create(attachment.proxyUrl)).GET().build()
				val response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())

				require(response.statusCode() in 200..299) {
					"Could not download ${attachment.fileName}."
				}

				images.add(
					CrosspostImage(
						fileName = attachment.fileName,
						contentType = attachment.contentType ?: "application/octet-stream",
						description = attachment.description.orEmpty(),
						width = attachment.width,
						height = attachment.height,
						data = response.body()
					)
				)
			}

			images
		}
	}

	private fun removeExpiredDrafts() {
		for ((id, draft) in drafts) {
			if (isExpired(draft)) {
				drafts.remove(id, draft)
			}
		}
	}

	private fun isExpired(draft: CrosspostDraft): Boolean {
		return System.currentTimeMillis() - draft.createdAtMillis > DRAFT_LIFETIME_MILLIS
	}

	private const val MAX_IMAGES = 10
	private const val MAX_TOTAL_IMAGE_BYTES = 20L * 1024 * 1024
	private const val DRAFT_LIFETIME_MILLIS = 15L * 60 * 1000
}
