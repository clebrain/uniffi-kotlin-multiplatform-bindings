{%- let cbi = ci|get_callback_interface_definition(name) %}
{%- let type_name = cbi|type_name(ci) %}
{%- let foreign_callback_name = format!("ForeignCallback{}", canonical_type_name) %}

{% if self.include_once_check("CallbackInterfaceRuntime.kt") %}{% include "CallbackInterfaceRuntime.kt" %}{% endif %}

// Declaration and FfiConverters for {{ type_name }} Callback Interface

public interface {{ type_name }} {
    {% for meth in cbi.methods() -%}
    fun {{ meth.name()|fn_name }}({% call kt::arg_list_decl(meth) %})
    {%- match meth.return_type() -%}
    {%- when Some with (return_type) %}: {{ return_type|type_name(ci) -}}
    {%- else -%}
    {%- endmatch %}
    {% endfor %}
    companion object
}

// The ForeignCallback that is passed to Rust.
internal object {{ foreign_callback_name }} {
    @Suppress("TooGenericExceptionCaught")
    fun callback(handle: Handle, method: kotlin.Int, argsData: UBytePointer, argsLen: kotlin.Int, outBuf: RustBufferByReference): kotlin.Int {
        val cb = {{ ffi_converter_name }}.lift(handle)
        return when (method) {
            IDX_CALLBACK_FREE -> {
                {{ ffi_converter_name }}.drop(handle)
                // Successful return
                // See docs of ForeignCallback in `uniffi_core/src/ffi/foreigncallbacks.rs`
                UNIFFI_CALLBACK_SUCCESS
            }
            {% for meth in cbi.methods() -%}
            {% let method_name = format!("invoke_{}", meth.name())|fn_name -%}
            {{ loop.index }} -> {
                // Call the method, write to outBuf and return a status code
                // See docs of ForeignCallback in `uniffi_core/src/ffi/foreigncallbacks.rs` for info
                try {
                    this.{{ method_name }}(cb, argsData, argsLen, outBuf)
                } catch (e: Throwable) {
                    // Unexpected error
                    try {
                        // Try to serialize the error into a string
                        outBuf.setValue({{ Type::String.borrow()|ffi_converter_name }}.lower(e.toString()))
                    } catch (e: Throwable) {
                        // If that fails, then it's time to give up and just return
                    }
                    UNIFFI_CALLBACK_UNEXPECTED_ERROR
                }
            }
            {% endfor %}
            else -> {
                // An unexpected error happened.
                // See docs of ForeignCallback in `uniffi_core/src/ffi/foreigncallbacks.rs`
                try {
                    // Try to serialize the error into a string
                    outBuf.setValue({{ Type::String.borrow()|ffi_converter_name }}.lower("Invalid Callback index"))
                } catch (e: Throwable) {
                    // If that fails, then it's time to give up and just return
                }
                UNIFFI_CALLBACK_UNEXPECTED_ERROR
            }
        }
    }

    {% for meth in cbi.methods() -%}
    {% let method_name = format!("invoke_{}", meth.name())|fn_name %}
    @Suppress("UNUSED_PARAMETER")
    private fun {{ method_name }}(kotlinCallbackInterface: {{ type_name }}, argsData: UBytePointer, argsLen: kotlin.Int, outBuf: RustBufferByReference): kotlin.Int {
        {%- if meth.arguments().len() > 0 %}
        val argsBuf = argsData.asSource(argsLen.toLong())
        {%- endif %}

        {%- match meth.return_type() %}
        {%- when Some with (return_type) %}
        fun makeCall() : Int {
            val returnValue = kotlinCallbackInterface.{{ meth.name()|fn_name }}(
                {%- for arg in meth.arguments() %}
                {{ arg|read_fn }}(argsBuf)
                {% if !loop.last %}, {% endif %}
                {%- endfor %}
            )
            outBuf.setValue({{ return_type|ffi_converter_name }}.lowerIntoRustBuffer(returnValue))
            return UNIFFI_CALLBACK_SUCCESS
        }
        {%- when None %}
        fun makeCall() : Int {
            kotlinCallbackInterface.{{ meth.name()|fn_name }}(
                {%- for arg in meth.arguments() %}
                {{ arg|read_fn }}(argsBuf)
                {%- if !loop.last %}, {% endif %}
                {%- endfor %}
            )
            return UNIFFI_CALLBACK_SUCCESS
        }
        {%- endmatch %}

        {%- match meth.throws_type() %}
        {%- when None %}
        fun makeCallAndHandleError() : Int = makeCall()
        {%- when Some(error_type) %}
        fun makeCallAndHandleError()  : Int = try {
            makeCall()
        } catch (e: {{ error_type|type_name(ci) }}) {
            // Expected error, serialize it into outBuf
            outBuf.setValue({{ error_type|ffi_converter_name }}.lowerIntoRustBuffer(e))
            UNIFFI_CALLBACK_ERROR
        }
        {%- endmatch %}

        return makeCallAndHandleError()
    }
    {% endfor %}
}

// The ffiConverter which transforms the Callbacks in to Handles to pass to Rust.
internal object {{ ffi_converter_name }}: FfiConverterCallbackInterface<{{ type_name }}>() {
    // prevent the callback from being GC'ed
    private val foreignCallback = {{ foreign_callback_name }}.toForeignCallback()

    override fun register(lib: UniFFILib) {
        rustCall { status: RustCallStatus ->
            lib.{{ cbi.ffi_init_callback().name() }}(this.foreignCallback, status)
        }
    }
}

internal expect fun {{ foreign_callback_name }}.toForeignCallback() : ForeignCallback
