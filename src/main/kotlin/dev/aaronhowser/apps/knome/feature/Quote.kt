package org.example.dev.aaronhowser.apps.knome.feature

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
			.setDescription(message)
			.setFooter("- $user")

		if (this is QuoteWithId) {
			embedBuilder.setTitle("Quote #$id")
		}

		return embedBuilder.build()
	}

}