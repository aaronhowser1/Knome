package dev.aaronhowser.apps.knome.crosspost

import dev.aaronhowser.apps.knome.discord.AaronServer
import dev.aaronhowser.apps.knome.discord.await
import net.dv8tion.jda.api.JDA

object CrosspostParentResolver {

	suspend fun resolve(messageLink: String, jda: JDA): CrosspostParent {
		val match = MESSAGE_LINK.matchEntire(messageLink.trim())
			?: throw IllegalArgumentException("The parent must be a Discord message link.")
		val channelId = match.groupValues[2].toLong()
		require(channelId == AaronServer.PHILOSOPHY_CROSSPOSTS_CHANNEL_ID) {
			"The parent must be a message from the philosophy crossposts channel."
		}

		val messageId = match.groupValues[3].toLong()
		val message = AaronServer.getPhilosophyCrossposts(jda).retrieveMessageById(messageId).await()
		require(message.author.idLong == jda.selfUser.idLong) {
			"The parent crosspost message was not created by Knome."
		}
		val embed = message.embeds.singleOrNull()
			?: throw IllegalArgumentException("The parent crosspost message does not contain one crosspost record.")
		require(embed.title == "Cross-post") {
			"The parent message is not a Knome crosspost record."
		}

		val blueskyUrl = embed.fields.firstOrNull { field -> field.name == "Bluesky" }
			?.value
			?.takeIf { value -> value.startsWith("https://bsky.app/") }
		val tumblrUrl = embed.fields.firstOrNull { field -> field.name == "Tumblr" }
			?.value
			?.takeIf { value -> value.startsWith("https://") && ".tumblr.com/post/" in value }
		require(blueskyUrl != null || tumblrUrl != null) {
			"The parent crosspost message has no successful Bluesky or Tumblr post."
		}
		return CrosspostParent(blueskyUrl, tumblrUrl)
	}

	private val MESSAGE_LINK = Regex("""https://(?:canary\.|ptb\.)?discord(?:app)?\.com/channels/(\d+)/(\d+)/(\d+)""")
}