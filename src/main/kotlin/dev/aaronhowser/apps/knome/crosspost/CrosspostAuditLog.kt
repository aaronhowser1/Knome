package dev.aaronhowser.apps.knome.crosspost

import dev.aaronhowser.apps.knome.discord.AaronServer
import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA

object CrosspostAuditLog {

	fun publish(jda: JDA, content: String) {
		val embed = EmbedBuilder()
			.setTitle("Cross-post")
			.setColor(0x7289DA)
			.setDescription("Messages reposted from Discord:\n\n$content")
			.addField("Tumblr", "todo", true)
			.addField("Bluesky", "todo", true)
			.setFooter("Knome Bot")
			.build()

		AaronServer.getModlog(jda)
			.sendMessageEmbeds(embed)
			.queue()
	}

}