{%- let type_name = type_|type_name(ci) %}
{%- let ffi_converter_name = type_|ffi_converter_name %}
{%- let canonical_type_name = type_|canonical_name %}

{% if e.is_flat() %}
sealed class {{ type_name }}(message: kotlin.String): Exception(message){% if contains_object_references %}, Disposable {% endif %} {
        // Each variant is a nested class
        // Flat enums carries a string error message, so no special implementation is necessary.
        {% for variant in e.variants() -%}
        class {{ variant|error_variant_name }}(message: kotlin.String) : {{ type_name }}(message)
        {% endfor %}

    internal companion object ErrorHandler : CallStatusErrorHandler<{{ type_name }}> {
        override fun lift(errorBuffer: RustBuffer): {{ type_name }} = {{ ffi_converter_name }}.lift(errorBuffer)
    }
}
{%- else %}
sealed class {{ type_name }}: Exception(){% if contains_object_references %}, Disposable {% endif %} {
    {% for variant in e.variants() -%}
    {%- let variant_name = variant|error_variant_name %}
    class {{ variant_name }}(
        {% for field in variant.fields() -%}
        val {{ field.name()|var_name }}: {{ field|type_name(ci) }}{% if loop.last %}{% else %}, {% endif %}
        {% endfor -%}
    ) : {{ type_name }}() {
        override val message
            get() = "{%- for field in variant.fields() %}{{ field.name()|var_name|unquote }}=${ {{field.name()|var_name }} }{% if !loop.last %}, {% endif %}{% endfor %}"
    }
    {% endfor %}

    internal companion object ErrorHandler : CallStatusErrorHandler<{{ type_name }}> {
        override fun lift(errorBuffer: RustBuffer): {{ type_name }} = {{ ffi_converter_name }}.lift(errorBuffer)
    }

    {% if contains_object_references %}
    @Suppress("UNNECESSARY_SAFE_CALL") // codegen is much simpler if we unconditionally emit safe calls here
    override fun destroy() {
        when(this) {
            {%- for variant in e.variants() %}
            is {{ type_name }}.{{ variant|error_variant_name }} -> {
                {%- if variant.has_fields() %}
                {% call kt::destroy_fields(variant) %}
                {% else -%}
                {%- endif %}
            }
            {%- endfor %}
        }
    }
    {% endif %}
}
{%- endif %}

internal object {{ ffi_converter_name }} : FfiConverterRustBuffer<{{ type_name }}> {
    override fun read(buf: NoCopySource): {{ type_name }} {
        {% if e.is_flat() %}
        return when(buf.readInt()) {
            {%- for variant in e.variants() %}
            {{ loop.index }} -> {{ type_name }}.{{ variant|error_variant_name }}({{ Type::String.borrow()|read_fn }}(buf))
            {%- endfor %}
            else -> throw RuntimeException("invalid error enum value, something is very wrong!!")
        }
        {% else %}

        return when(buf.readInt()) {
            {%- for variant in e.variants() %}
            {{ loop.index }} -> {{ type_name }}.{{ variant|error_variant_name }}({% if variant.has_fields() %}
                {% for field in variant.fields() -%}
                {{ field|read_fn }}(buf),
                {% endfor -%}
            {%- endif -%})
            {%- endfor %}
            else -> throw RuntimeException("invalid error enum value, something is very wrong!!")
        }
        {%- endif %}
    }

    override fun allocationSize(value: {{ type_name }}): kotlin.Int {
        {%- if e.is_flat() %}
        return 4
        {%- else %}
        return when(value) {
            {%- for variant in e.variants() %}
            is {{ type_name }}.{{ variant|error_variant_name }} -> (
                // Add the size for the Int that specifies the variant plus the size needed for all fields
                4
                {%- for field in variant.fields() %}
                + {{ field|allocation_size_fn }}(value.{{ field.name()|var_name }})
                {%- endfor %}
            )
            {%- endfor %}
        }
        {%- endif %}
    }

    override fun write(value: {{ type_name }}, buf: Buffer) {
        when(value) {
            {%- for variant in e.variants() %}
            is {{ type_name }}.{{ variant|error_variant_name }} -> {
                buf.writeInt({{ loop.index }})
                {%- for field in variant.fields() %}
                {{ field|write_fn }}(value.{{ field.name()|var_name }}, buf)
                {%- endfor %}
                Unit
            }
            {%- endfor %}
        }
    }

}
