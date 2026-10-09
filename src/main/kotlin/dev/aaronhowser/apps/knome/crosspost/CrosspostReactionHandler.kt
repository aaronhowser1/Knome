package dev.aaronhowser.apps.knome.crosspost

import dev.aaronhowser.apps.knome.KnomeBot
import dev.aaronhowser.apps.knome.crosspost.model.CrosspostDestination
import dev.aaronhowser.apps.knome.crosspost.model.CrosspostParent
import dev.aaronhowser.apps.knome.crosspost.persistence.CrosspostRepository
import dev.aaronhowser.apps.knome.crosspost.service.CrosspostDraftService
import dev.aaronhowser.apps.knome.crosspost.service.CrosspostPublishingService
import dev.aaronhowser.apps.knome.discord.AaronServer
import dev.aaronhowser.apps.knome.discord.await
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent

object CrosspostReactionHandler {

	private val publishingMutex = Mutex()

	suspend fun handle(event: MessageReactionAddEvent, replyToMostRecent: Boolean) {
		try {
			publishingMutex.withLock {
				handleReaction(event, replyToMostRecent)
			}
		} catch (exception: Exception) {
			KnomeBot.LOGGER.severe("Reaction crosspost failed: ${exception.stackTraceToString()}")
			CrosspostAuditLog.publishRejection(
				event.jda,
				event.jumpUrl,
				"Could not publish the crosspost: ${exception.message ?: exception.javaClass.simpleName}"
			)
		} finally {
			removeReaction(event)
		}
	}

	private suspend fun handleReaction(event: MessageReactionAddEvent, replyToMostRecent: Boolean) {
		val message = event.retrieveMessage().await()
		if (message.author.idLong != AaronServer.AARON_MEMBER_ID) {
			CrosspostAuditLog.publishRejection(
				event.jda,
				message.jumpUrl,
				"Only Aaron's messages can be crossposted."
			)
			return
		}

		val originalPublications = CrosspostRepository.getPublications(message.idLong)
		if (originalPublications != null) {
			CrosspostAuditLog.publishRejection(
				event.jda,
				message.jumpUrl,
				"This message has already been reposted.",
				originalPublications
			)
			return
		}

		val parent = if (replyToMostRecent) getMostRecentParent() else null
		if (replyToMostRecent && parent == null) {
			CrosspostAuditLog.publishRejection(
				event.jda,
				message.jumpUrl,
				"There is no complete Tumblr and Bluesky repost to reply to."
			)
			return
		}

		try {
			val draft = CrosspostDraftService.prepare(
				AaronServer.AARON_MEMBER_ID,
				message.idLong,
				message.idLong,
				event.channel
			)
			CrosspostDraftService.discardDraft(draft.id, AaronServer.AARON_MEMBER_ID)

			val results = CrosspostPublishingService.publish(draft, CrosspostDestination.BOTH, parent)
			CrosspostRepository.recordSuccessfulPublications(draft, results)
			CrosspostAuditLog.publish(event.jda, draft, results)
		} catch (exception: Exception) {
			KnomeBot.LOGGER.severe("Reaction crosspost failed: ${exception.stackTraceToString()}")
			CrosspostAuditLog.publishRejection(
				event.jda,
				message.jumpUrl,
				"Could not publish the crosspost: ${exception.message ?: exception.javaClass.simpleName}"
			)
		}
	}

	private fun getMostRecentParent(): CrosspostParent? {
		val parent = CrosspostRepository.getMostRecentPublications()
		if (parent.blueskyUrl == null || parent.tumblrUrl == null) {
			return null
		}
		return parent
	}

	private suspend fun removeReaction(event: MessageReactionAddEvent) {
		try {
			val user = event.user ?: event.retrieveUser().await()
			event.reaction.removeReaction(user).await()
		} catch (exception: Exception) {
			KnomeBot.LOGGER.warning("Could not remove crosspost reaction: ${exception.message}")
		}
	}
}