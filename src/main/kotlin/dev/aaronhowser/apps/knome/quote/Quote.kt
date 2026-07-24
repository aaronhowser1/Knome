package dev.aaronhowser.apps.knome.quote

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.entities.MessageEmbed

sealed class Quote {

	abstract val user: String
	abstract val message: String

	data class QuoteWithoutId(
		override val user: String,
		override val message: String
	) : Quote()

	data class QuoteWithId(
		val id: Int,
		override val user: String,
		override val message: String
	) : Quote()

	fun getEmbed(): MessageEmbed {
		val embedBuilder = EmbedBuilder()
			.setDescription(message.take(MessageEmbed.DESCRIPTION_MAX_LENGTH))
			.setFooter("- ${user.take(MessageEmbed.TEXT_MAX_LENGTH - 2)}")

		if (this is QuoteWithId) {
			embedBuilder.setTitle("Quote #$id")
		}

		return embedBuilder.build()
	}

	companion object {
		fun List<Quote>.getEmbedDescription(): String {
			val description = StringBuilder()

			for ((index, quote) in withIndex()) {
				if (quote is QuoteWithId) {
					description.append("**#${quote.id}**")
				}

				description.append("\n  ${quote.message}")
				description.append("\n  \\- *${quote.user}*")

				if (index < lastIndex) {
					description.append("\n\n")
				}

				if (description.length >= MessageEmbed.DESCRIPTION_MAX_LENGTH) {
					return description.take(MessageEmbed.DESCRIPTION_MAX_LENGTH).toString()
				}
			}

			return description.toString()
		}
	}

}