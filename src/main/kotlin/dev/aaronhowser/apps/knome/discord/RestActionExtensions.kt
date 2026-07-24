package dev.aaronhowser.apps.knome.discord

import kotlinx.coroutines.suspendCancellableCoroutine
import net.dv8tion.jda.api.requests.RestAction
import kotlin.coroutines.resumeWithException

suspend fun <T> RestAction<T>.await(): T = suspendCancellableCoroutine { continuation ->
		queue(
			{ result -> continuation.resume(result) { _, _, _ -> } },
			{ error -> continuation.resumeWithException(error) }
		)
	}