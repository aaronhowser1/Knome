package dev.aaronhowser.apps.knome.crosspost

data class CrosspostDraft(
	val id: String,
	val ownerId: Long,
	val messages: List<String>,
	val images: List<CrosspostImage>,
	val createdAtMillis: Long
) {
	val content: String
		get() = messages.joinToString("\n\n").trim()
}

data class CrosspostImage(
	val fileName: String,
	val contentType: String,
	val description: String,
	val width: Int,
	val height: Int,
	val data: ByteArray
)

enum class CrosspostDestination {
	TUMBLR,
	BLUESKY,
	BOTH
}

data class CrosspostResult(
	val destination: String,
	val url: String? = null,
	val error: String? = null
) {
	val succeeded: Boolean
		get() = error == null
}