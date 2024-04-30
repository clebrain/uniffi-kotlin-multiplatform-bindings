// Async return type handlers

private const val UNIFFI_RUST_FUTURE_POLL_READY = 0.toShort()
private const val UNIFFI_RUST_FUTURE_POLL_MAYBE_READY = 1.toShort()

private val uniffiContinuationHandleMap = UniFfiHandleMap<kotlinx.coroutines.CancellableContinuation<kotlin.Short>>()

internal fun resumeContinuation(continuationHandle: kotlin.ULong, pollResult: kotlin.Short) {
    uniffiContinuationHandleMap.remove(continuationHandle)?.resume(pollResult)
}

@kotlin.Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect class UniFfiRustFutureContinuationCallbackType

internal expect fun createUniFfiRustFutureContinuationCallback(): UniFfiRustFutureContinuationCallbackType

// FFI type for Rust future continuations
internal val uniffiRustFutureContinuationCallback = createUniFfiRustFutureContinuationCallback()

internal fun registerUniffiRustFutureContinuationCallback(lib: UniFFILib) {
    lib.{{ ci.ffi_rust_future_continuation_callback_set().name() }}(uniffiRustFutureContinuationCallback)
}

internal suspend fun<T, F, E: kotlin.Exception> uniffiRustCallAsync(
    rustFuture: Pointer,
    pollFunc: (Pointer, kotlin.ULong) -> kotlin.Unit,
    completeFunc: (Pointer, RustCallStatus) -> F,
    freeFunc: (Pointer) -> kotlin.Unit,
    liftFunc: (F) -> T,
    errorHandler: CallStatusErrorHandler<E>
): T {
    try {
        do {
            val pollResult = kotlinx.coroutines.suspendCancellableCoroutine<kotlin.Short> { continuation ->
                pollFunc(
                    rustFuture,
                    uniffiContinuationHandleMap.insert(continuation)
                )
            }
        } while (pollResult != UNIFFI_RUST_FUTURE_POLL_READY);

        return liftFunc(
            rustCallWithError(errorHandler) { status -> completeFunc(rustFuture, status) }
        )
    } finally {
        freeFunc(rustFuture)
    }
}
