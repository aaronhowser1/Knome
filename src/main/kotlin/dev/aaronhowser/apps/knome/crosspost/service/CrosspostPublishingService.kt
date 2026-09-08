package dev.aaronhowser.apps.knome.crosspost.service

import dev.aaronhowser.apps.knome.crosspost.model.CrosspostDestination
import dev.aaronhowser.apps.knome.crosspost.model.CrosspostDraft
import dev.aaronhowser.apps.knome.crosspost.model.CrosspostParent
import dev.aaronhowser.apps.knome.crosspost.model.CrosspostResult
import dev.aaronhowser.apps.knome.crosspost.platform.BlueskyPublisher
import dev.aaronhowser.apps.knome.crosspost.platform.TumblrPublisher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

object CrosspostPublishingService {

	suspend fun publish(
		draft: CrosspostDraft,
		destination: CrosspostDestination,
		parent: CrosspostParent?
	): List<CrosspostResult> {
		return coroutineScope {
			val publishers = mutableListOf<suspend () -> CrosspostResult>()
			if (destination == CrosspostDestination.TUMBLR || destination == CrosspostDestination.BOTH) {
				publishers.add { TumblrPublisher.publish(draft, parent?.tumblrUrl) }
			}
			if (destination == CrosspostDestination.BLUESKY || destination == CrosspostDestination.BOTH) {
				publishers.add { BlueskyPublisher.publish(draft, parent?.blueskyUrl) }
			}
			publishers.map { publisher -> async { publisher() } }.awaitAll()
		}
	}
}