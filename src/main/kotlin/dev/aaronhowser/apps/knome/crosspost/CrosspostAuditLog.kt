package dev.aaronhowser.apps.knome.crosspost

import dev.aaronhowser.apps.knome.discord.AaronServer
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA

object CrosspostAuditLog {

	fun publish(jda: JDA, content: String, results: List<CrosspostResult>) {
		val embed = EmbedBuilder()
			.setTitle("Cross-post")
			.setColor(if (results.all { result -> result.succeeded }) 0x57F287 else 0xED4245)
			.setDescription(content.take(3500).ifBlank { "(images only)" })
			.setFooter("Knome Bot")

		for (result in results) {
			val value = result.url ?: "Failed: ${result.error}"
			embed.addField(result.destination, value.take(1000), false)
		}

		AaronServer.getModlog(jda).sendMessageEmbeds(embed.build()).queue()
	}
}