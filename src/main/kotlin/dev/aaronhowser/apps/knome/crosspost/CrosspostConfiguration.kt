package dev.aaronhowser.apps.knome.crosspost

object CrosspostConfiguration {

	val blueskyVariables = listOf(
		"KNOME_BLUESKY_IDENTIFIER",
		"KNOME_BLUESKY_APP_PASSWORD"
	)

	val tumblrVariables = listOf(
		"KNOME_TUMBLR_BLOG",
		"KNOME_TUMBLR_CONSUMER_KEY",
		"KNOME_TUMBLR_CONSUMER_SECRET",
		"KNOME_TUMBLR_ACCESS_TOKEN",
		"KNOME_TUMBLR_ACCESS_TOKEN_SECRET"
	)

	val requiredVariables: List<String>
		get() = blueskyVariables + tumblrVariables

	fun isConfigured(name: String): Boolean {
		return !System.getenv(name).isNullOrBlank()
	}

	fun requireConfigured() {
		val missing = requiredVariables.filterNot { name -> isConfigured(name) }
		require(missing.isEmpty()) {
			"Crossposting is not configured. Missing: ${missing.joinToString(", ")}. Run /crosspost-status for details."
		}
	}

	fun statusMessage(): String {
		return buildString {
			appendLine("Crosspost environment:")
			appendStatus("Bluesky", blueskyVariables)
			appendLine()
			appendStatus("Tumblr", tumblrVariables)
		}.trim()
	}

	private fun StringBuilder.appendStatus(platform: String, variables: List<String>) {
		val configured = variables.all { name -> isConfigured(name) }
		appendLine("${if (configured) "✅" else "❌"} $platform")
		for (name in variables) {
			appendLine("${if (isConfigured(name)) "✅" else "❌"} `$name`")
		}
	}
}