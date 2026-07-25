package dev.aaronhowser.apps.knome.crosspost

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.util.Base64
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TumblrPublisher {

	private val httpClient = HttpClient.newHttpClient()
	private val json = Json { ignoreUnknownKeys = true }

	suspend fun publish(draft: CrosspostDraft): CrosspostResult {
		return try {
			withContext(Dispatchers.IO) {
				val credentials = TumblrCredentials.fromEnvironment()
				val url = "https://api.tumblr.com/v2/blog/${encode(credentials.blogIdentifier)}/posts"
				val boundary = "Knome-${UUID.randomUUID()}"
				val requestBody = createMultipartBody(draft, boundary)
				val authorization = createAuthorization(url, credentials)
				val request = HttpRequest.newBuilder(URI.create(url))
					.header("Authorization", authorization)
					.header("Content-Type", "multipart/form-data; boundary=$boundary")
					.POST(HttpRequest.BodyPublishers.ofByteArray(requestBody))
					.build()
				val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
				require(response.statusCode() in 200..299) { tumblrError(response) }
				val responseObject = json.parseToJsonElement(response.body()).jsonObject
				val postId = responseObject["response"]!!.jsonObject["id"]!!.jsonPrimitive.content
				val blogName = credentials.blogIdentifier.substringBefore(".tumblr.com")
				CrosspostResult("Tumblr", "https://$blogName.tumblr.com/post/$postId")
			}
		} catch (exception: Exception) {
			CrosspostResult("Tumblr", error = exception.message ?: exception.javaClass.simpleName)
		}
	}

	private fun createMultipartBody(draft: CrosspostDraft, boundary: String): ByteArray {
		val identifiers = draft.images.indices.map { index -> "image-$index" }
		val content = buildJsonArray {
			for (message in draft.messages) {
				add(buildJsonObject {
					put("type", "text")
					put("text", message)
				})
			}
			for (index in draft.images.indices) {
				val image = draft.images[index]
				add(buildJsonObject {
					put("type", "image")
					put("media", buildJsonArray {
						add(buildJsonObject {
							put("type", image.contentType)
							put("identifier", identifiers[index])
							put("width", image.width)
							put("height", image.height)
						})
					})
					if (image.description.isNotBlank()) {
						put("alt_text", image.description)
					}
				})
			}
		}
		val body = buildJsonObject {
			put("content", content)
			put("state", "published")
		}.toString()

		val output = mutableListOf<Byte>()
		appendPart(output, boundary, "json", null, "application/json", body.toByteArray())
		for (index in draft.images.indices) {
			val image = draft.images[index]
			appendPart(output, boundary, identifiers[index], image.fileName, image.contentType, image.data)
		}
		append(output, "--$boundary--\r\n".toByteArray())
		return output.toByteArray()
	}

	private fun appendPart(
		output: MutableList<Byte>,
		boundary: String,
		name: String,
		fileName: String?,
		contentType: String,
		data: ByteArray
	) {
		val disposition = if (fileName == null) {
			"Content-Disposition: form-data; name=\"$name\"\r\n"
		} else {
			"Content-Disposition: form-data; name=\"$name\"; filename=\"${fileName.replace("\"", "")}\"\r\n"
		}
		append(output, "--$boundary\r\n".toByteArray())
		append(output, disposition.toByteArray())
		append(output, "Content-Type: $contentType\r\n\r\n".toByteArray())
		append(output, data)
		append(output, "\r\n".toByteArray())
	}

	private fun append(output: MutableList<Byte>, bytes: ByteArray) {
		for (byte in bytes) {
			output.add(byte)
		}
	}

	private fun createAuthorization(url: String, credentials: TumblrCredentials): String {
		val oauth = sortedMapOf(
			"oauth_consumer_key" to credentials.consumerKey,
			"oauth_nonce" to UUID.randomUUID().toString().replace("-", ""),
			"oauth_signature_method" to "HMAC-SHA1",
			"oauth_timestamp" to Instant.now().epochSecond.toString(),
			"oauth_token" to credentials.accessToken,
			"oauth_version" to "1.0"
		)
		val parameterString = oauth.entries.joinToString("&") { (key, value) -> "${encode(key)}=${encode(value)}" }
		val signatureBase = "POST&${encode(url)}&${encode(parameterString)}"
		val signingKey = "${encode(credentials.consumerSecret)}&${encode(credentials.accessTokenSecret)}"
		val mac = Mac.getInstance("HmacSHA1")
		mac.init(SecretKeySpec(signingKey.toByteArray(), "HmacSHA1"))
		val signature = Base64.getEncoder().encodeToString(mac.doFinal(signatureBase.toByteArray()))
		oauth["oauth_signature"] = signature
		return "OAuth " + oauth.entries.joinToString(", ") { (key, value) -> "$key=\"${encode(value)}\"" }
	}

	private fun tumblrError(response: HttpResponse<String>): String {
		val message = try {
			val root = json.parseToJsonElement(response.body()).jsonObject
			root["errors"]?.jsonObject?.toString() ?: root["meta"]?.jsonObject?.get("msg")?.jsonPrimitive?.content
		} catch (_: Exception) {
			null
		}
		return "Tumblr returned ${response.statusCode()}${message?.let { ": $it" }.orEmpty()}."
	}

	private fun encode(value: String): String {
		return URLEncoder.encode(value, StandardCharsets.UTF_8)
			.replace("+", "%20")
			.replace("%7E", "~")
	}

	private data class TumblrCredentials(
		val blogIdentifier: String,
		val consumerKey: String,
		val consumerSecret: String,
		val accessToken: String,
		val accessTokenSecret: String
	) {
		companion object {
			fun fromEnvironment(): TumblrCredentials {
				return TumblrCredentials(
					blogIdentifier = environment("KNOME_TUMBLR_BLOG"),
					consumerKey = environment("KNOME_TUMBLR_CONSUMER_KEY"),
					consumerSecret = environment("KNOME_TUMBLR_CONSUMER_SECRET"),
					accessToken = environment("KNOME_TUMBLR_ACCESS_TOKEN"),
					accessTokenSecret = environment("KNOME_TUMBLR_ACCESS_TOKEN_SECRET")
				)
			}

			private fun environment(name: String): String {
				return System.getenv(name)?.takeIf { value -> value.isNotBlank() }
					?: error("$name is not configured.")
			}
		}
	}
}