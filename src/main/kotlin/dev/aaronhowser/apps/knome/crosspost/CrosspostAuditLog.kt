package dev.aaronhowser.apps.knome.crosspost

import dev.aaronhowser.apps.knome.discord.AaronServer
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA

object CrosspostAuditLog {

	fun publish(jda: JDA, draft: CrosspostDraft, results: List<CrosspostResult>) {
		val embed = EmbedBuilder()
			.setTitle("Cross-post")
			.setColor(if (results.all { result -> result.succeeded }) 0x57F287 else 0xED4245)
			.setDescription(draft.content.take(3500).ifBlank { "(images only)" })
			.setFooter("Knome Bot")

		val messageLinks = draft.messageLinks.mapIndexed { index, link -> "[Message ${index + 1}]($link)" }
		val linkGroups = mutableListOf<MutableList<String>>()
		for (messageLink in messageLinks) {
			val currentGroup = linkGroups.lastOrNull()
			if (currentGroup == null || (currentGroup + messageLink).joinToString(" · ").length > 1000) {
				linkGroups.add(mutableListOf(messageLink))
			} else {
				currentGroup.add(messageLink)
			}
		}
		for (index in linkGroups.indices) {
			val fieldName = if (index == 0) "Discord messages" else "Discord messages (continued)"
			embed.addField(fieldName, linkGroups[index].joinToString(" · "), false)
		}

		for (result in results) {
			val value = result.url ?: "Failed: ${result.error}"
			embed.addField(result.destination, value.take(1000), false)
		}

		AaronServer.getModlog(jda).sendMessageEmbeds(embed.build()).queue()
	}
}