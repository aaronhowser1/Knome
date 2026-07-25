package dev.aaronhowser.apps.knome.crosspost

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.text.BreakIterator
import java.time.Instant
import java.util.Locale

object BlueskyPublisher {

	private val httpClient = HttpClient.newHttpClient()
	private val json = Json { ignoreUnknownKeys = true }

	fun previewParts(draft: CrosspostDraft): List<String> {
		val parts = splitText(draft.content).toMutableList()

		if (parts.isEmpty()) {
			parts.add("")
		}

		while (parts.size * IMAGES_PER_POST < draft.images.size) {
			parts.add("")
		}

		return parts
	}

	suspend fun publish(draft: CrosspostDraft): CrosspostResult {
		return try {
			withContext(Dispatchers.IO) {
				val identifier = environment(CrosspostConfiguration.BLUESKY_IDENTIFIER)
				val password = environment(CrosspostConfiguration.BLUESKY_APP_PASSWORD)
				val session = createSession(identifier, password)
				val imageBlobs = uploadImages(session.accessToken, draft.images)

				val parts = previewParts(draft)
				var root: RecordReference? = null
				var parent: RecordReference? = null

				for (index in parts.indices) {
					val images = imageBlobs
						.drop(index * IMAGES_PER_POST)
						.take(IMAGES_PER_POST)

					val record = createPost(session, parts[index], images, root, parent)

					if (root == null) {
						root = record
					}

					parent = record
				}

				val postKey = root!!.uri.substringAfterLast('/')
				CrosspostResult("Bluesky", "https://bsky.app/profile/${session.handle}/post/$postKey")
			}
		} catch (exception: Exception) {
			CrosspostResult("Bluesky", error = exception.message ?: exception.javaClass.simpleName)
		}
	}

	private fun createSession(identifier: String, password: String): BlueskySession {
		val body = buildJsonObject {
			put("identifier", identifier)
			put("password", password)
		}

		val response = postJson("$SERVICE/xrpc/com.atproto.server.createSession", body)

		return BlueskySession(
			handle = response["handle"]!!.jsonPrimitive.content,
			did = response["did"]!!.jsonPrimitive.content,
			accessToken = response["accessJwt"]!!.jsonPrimitive.content
		)
	}

	private fun uploadImages(accessToken: String, images: List<CrosspostImage>): List<UploadedImage> {
		val uploaded = mutableListOf<UploadedImage>()

		for (image in images) {
			require(image.data.size <= MAX_IMAGE_BYTES) {
				"${image.fileName} is larger than Bluesky's 2 MB image limit."
			}

			val request = HttpRequest.newBuilder(URI.create("$SERVICE/xrpc/com.atproto.repo.uploadBlob"))
				.header("Authorization", "Bearer $accessToken")
				.header("Content-Type", image.contentType)
				.POST(HttpRequest.BodyPublishers.ofByteArray(image.data))
				.build()

			val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
			require(response.statusCode() in 200..299) { apiError("Bluesky image upload", response) }

			val blob = json.parseToJsonElement(response.body()).jsonObject["blob"]!!.jsonObject
			uploaded.add(UploadedImage(blob, image.description))
		}

		return uploaded
	}

	private fun createPost(
		session: BlueskySession,
		text: String,
		images: List<UploadedImage>,
		root: RecordReference?,
		parent: RecordReference?
	): RecordReference {
		val record = buildJsonObject {
			put($$"$type", "app.bsky.feed.post")
			put("text", text)
			put("createdAt", Instant.now().toString())

			if (images.isNotEmpty()) {
				put("embed", buildJsonObject {
					put($$"$type", "app.bsky.embed.images")
					put("images", buildJsonArray {
						for (image in images) {
							add(buildJsonObject {
								put("alt", image.alt)
								put("image", image.blob)
							})
						}
					})
				})
			}

			if (root != null && parent != null) {
				put("reply", buildJsonObject {
					put("root", root.toJson())
					put("parent", parent.toJson())
				})
			}
		}

		val body = buildJsonObject {
			put("repo", session.did)
			put("collection", "app.bsky.feed.post")
			put("record", record)
		}

		val response = postJson("$SERVICE/xrpc/com.atproto.repo.createRecord", body, session.accessToken)

		return RecordReference(
			uri = response["uri"]!!.jsonPrimitive.content,
			cid = response["cid"]!!.jsonPrimitive.content
		)
	}

	private fun postJson(url: String, body: JsonObject, accessToken: String? = null): JsonObject {
		val builder = HttpRequest.newBuilder(URI.create(url))
			.header("Content-Type", "application/json")

		if (accessToken != null) {
			builder.header("Authorization", "Bearer $accessToken")
		}

		val request = builder.POST(HttpRequest.BodyPublishers.ofString(body.toString())).build()
		val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

		require(response.statusCode() in 200..299) { apiError("Bluesky", response) }
		return json.parseToJsonElement(response.body()).jsonObject
	}

	private fun apiError(operation: String, response: HttpResponse<String>): String {
		val message = try {
			json.parseToJsonElement(response.body()).jsonObject["message"]?.jsonPrimitive?.content
		} catch (_: Exception) {
			null
		}

		return "$operation returned ${response.statusCode()}${message?.let { ": $it" }.orEmpty()}."
	}

	private fun splitText(text: String): List<String> {
		if (text.isBlank()) {
			return emptyList()
		}

		val parts = mutableListOf<String>()
		var remaining = text.trim()

		while (graphemeCount(remaining) > MAX_GRAPHEMES) {
			val boundary = preferredBoundary(remaining)
			parts.add(remaining.substring(0, boundary).trim())
			remaining = remaining.substring(boundary).trim()
		}

		if (remaining.isNotEmpty()) {
			parts.add(remaining)
		}

		return parts
	}

	private fun preferredBoundary(text: String): Int {
		val iterator = BreakIterator.getCharacterInstance(Locale.ROOT)
		iterator.setText(text)

		var boundary = iterator.first()
		repeat(MAX_GRAPHEMES) {
			val next = iterator.next()
			if (next == BreakIterator.DONE) {
				return text.length
			}
			boundary = next
		}

		val paragraphBreak = text.lastIndexOf("\n\n", boundary)
		if (paragraphBreak > 0) {
			return paragraphBreak
		}

		val whitespace = text.lastIndexOfAny(charArrayOf(' ', '\n', '\t'), boundary)
		return if (whitespace > 0) whitespace else boundary
	}

	private fun graphemeCount(text: String): Int {
		val iterator = BreakIterator.getCharacterInstance(Locale.ROOT)
		iterator.setText(text)

		var count = 0
		while (iterator.next() != BreakIterator.DONE) {
			count++
		}

		return count
	}

	private fun environment(name: String): String {
		return System.getenv(name)?.takeIf { value -> value.isNotBlank() }
			?: error("$name is not configured.")
	}

	private fun RecordReference.toJson(): JsonObject {
		return buildJsonObject {
			put("uri", uri)
			put("cid", cid)
		}
	}

	private data class BlueskySession(val handle: String, val did: String, val accessToken: String)
	private data class UploadedImage(val blob: JsonObject, val alt: String)
	private data class RecordReference(val uri: String, val cid: String)

	private const val SERVICE = "https://bsky.social"
	private const val MAX_GRAPHEMES = 300
	private const val IMAGES_PER_POST = 4
	private const val MAX_IMAGE_BYTES = 2 * 1024 * 1024

}
