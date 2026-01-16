package org.example.dev.aaronhowser.apps.knome.util

import kotlinx.coroutines.suspendCancellableCoroutine
import net.dv8tion.jda.api.requests.RestAction
import kotlin.coroutines.resumeWithException

object ExtensionFunctions {

	suspend fun <T> RestAction<T>.await(): T = suspendCancellableCoroutine { cont ->
		queue(
			{ result -> cont.resume(result) { throwable, t, context -> } },
			{ error -> cont.resumeWithException(error) }
		)
	}

}