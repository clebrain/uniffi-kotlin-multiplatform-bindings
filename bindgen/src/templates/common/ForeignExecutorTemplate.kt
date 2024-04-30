{{ self.add_import("kotlinx.coroutines.delay") }}
{{ self.add_import("kotlinx.coroutines.isActive") }}
{{ self.add_import("kotlinx.coroutines.launch") }}

internal const val UNIFFI_RUST_TASK_CALLBACK_SUCCESS = 0.toByte()
internal const val UNIFFI_RUST_TASK_CALLBACK_CANCELLED = 1.toByte()
internal const val UNIFFI_FOREIGN_EXECUTOR_CALLBACK_SUCCESS = 0.toByte()
internal const val UNIFFI_FOREIGN_EXECUTOR_CALLBACK_CANCELLED = 1.toByte()
internal const val UNIFFI_FOREIGN_EXECUTOR_CALLBACK_ERROR = 2.toByte()

// Callback function to execute a Rust task.
// The Kotlin code schedules these in a coroutine then invokes them.
// TODO remove suppress when https://youtrack.jetbrains.com/issue/KT-29819/New-rules-for-expect-actual-declarations-in-MPP is solved
@kotlin.Suppress("NO_ACTUAL_FOR_EXPECT")
internal expect class UniFfiForeignExecutorCallback

internal expect fun createUniFfiForeignExecutorCallback(): UniFfiForeignExecutorCallback

internal object FfiConverterForeignExecutor: FfiConverter<kotlinx.coroutines.CoroutineScope, kotlin.ULong> {
    internal val handleMap = UniFfiHandleMap<kotlinx.coroutines.CoroutineScope>()
    internal val foreignExecutorCallback = createUniFfiForeignExecutorCallback()

    internal fun drop(handle: kotlin.ULong) {
        handleMap.remove(handle)
    }

    internal fun register(lib: UniFFILib) {
        {%- match ci.ffi_foreign_executor_callback_set() %}
        {%- when Some with (fn) %}
        lib.{{ fn.name() }}(UniFfiForeignExecutorCallback)
        {%- when None %}
        {#- No foreign executor, we don't set anything #}
        {% endmatch %}
    }

    // Number of live handles, exposed so we can test the memory management
    internal fun handleCount() : kotlin.Int {
        return handleMap.size
    }

    override fun allocationSize(value: kotlinx.coroutines.CoroutineScope) = kotlin.ULong.SIZE_BYTES

    override fun lift(value: kotlin.ULong): kotlinx.coroutines.CoroutineScope {
        return handleMap.get(value) ?: throw kotlin.RuntimeException("unknown handle in FfiConverterForeignExecutor.lift")
    }

    override fun read(buf: NoCopySource): kotlinx.coroutines.CoroutineScope = TODO("unused")

    override fun lower(value: kotlinx.coroutines.CoroutineScope): kotlin.ULong {
        return handleMap.insert(value)
    }

    override fun write(value: kotlinx.coroutines.CoroutineScope, buf: okio.Buffer) = TODO("unused")

}
