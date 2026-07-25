package dev.aaronhowser.apps.knome.crosspost

object CrosspostConfiguration {

	const val BLUESKY_IDENTIFIER = "KNOME_BLUESKY_IDENTIFIER"
	const val BLUESKY_APP_PASSWORD = "KNOME_BLUESKY_APP_PASSWORD"
	const val TUMBLR_BLOG = "KNOME_TUMBLR_BLOG"
	const val TUMBLR_CONSUMER_KEY = "KNOME_TUMBLR_CONSUMER_KEY"
	const val TUMBLR_CONSUMER_SECRET = "KNOME_TUMBLR_CONSUMER_SECRET"
	const val TUMBLR_ACCESS_TOKEN = "KNOME_TUMBLR_ACCESS_TOKEN"
	const val TUMBLR_ACCESS_TOKEN_SECRET = "KNOME_TUMBLR_ACCESS_TOKEN_SECRET"

	val BLUESKY_VARIABLES = listOf(
		BLUESKY_IDENTIFIER,
		BLUESKY_APP_PASSWORD
	)

	val TUMBLR_VARIABLES = listOf(
		TUMBLR_BLOG,
		TUMBLR_CONSUMER_KEY,
		TUMBLR_CONSUMER_SECRET,
		TUMBLR_ACCESS_TOKEN,
		TUMBLR_ACCESS_TOKEN_SECRET
	)

	val REQUIRED_VARIABLES: List<String>
		get() = BLUESKY_VARIABLES + TUMBLR_VARIABLES

	fun isConfigured(name: String): Boolean {
		return !System.getenv(name).isNullOrBlank()
	}

	fun requireConfigured() {
		val missing = REQUIRED_VARIABLES.filterNot { name -> isConfigured(name) }
		require(missing.isEmpty()) {
			"Crossposting is not configured. Missing: ${missing.joinToString(", ")}. Run /crosspost-status for details."
		}
	}

	fun statusMessage(): String {
		return buildString {
			appendLine("Crosspost environment:")

			val platforms = listOf(
				"Bluesky" to BLUESKY_VARIABLES,
				"Tumblr" to TUMBLR_VARIABLES
			)

			for ((index, platform) in platforms.withIndex()) {
				if (index > 0) appendLine()
				val (platformName, variables) = platform
				val configured = variables.all { name -> isConfigured(name) }

				appendLine("${if (configured) "✅" else "❌"} $platformName")

				for (name in variables) {
					appendLine("${if (isConfigured(name)) "✅" else "❌"} `$name`")
				}
			}
		}.trim()
	}
}
