package dev.aaronhowser.apps.knome.discord

import kotlinx.coroutines.suspendCancellableCoroutine
import net.dv8tion.jda.api.requests.RestAction
import kotlin.coroutines.resumeWithException

suspend fun <T> RestAction<T>.await(): T = suspendCancellableCoroutine { continuation ->
	val task = submit()
	continuation.invokeOnCancellation { task.cancel(true) }

	task.whenComplete { result, error ->
		if (!continuation.isActive) {
			return@whenComplete
		}

		if (error == null) {
			continuation.resume(result) { _, _, _ -> }
		} else {
			continuation.resumeWithException(error)
		}
	}
}
