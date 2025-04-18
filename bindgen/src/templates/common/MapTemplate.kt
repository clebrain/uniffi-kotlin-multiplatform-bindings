{%- let key_type_name = key_type|type_name(ci) %}
{%- let value_type_name = value_type|type_name(ci) %}
internal object {{ ffi_converter_name }}: FfiConverterRustBuffer<Map<{{ key_type_name }}, {{ value_type_name }}>> {
    override fun read(buf: NoCopySource): Map<{{ key_type_name }}, {{ value_type_name }}> {
        val len = buf.readInt()
        return buildMap<{{ key_type_name }}, {{ value_type_name }}>(len) {
            repeat(len) {
                val k = {{ key_type|read_fn }}(buf)
                val v = {{ value_type|read_fn }}(buf)
                this[k] = v
            }
        }
    }

    override fun allocationSize(value: Map<{{ key_type_name }}, {{ value_type_name }}>): kotlin.Int {
        val spaceForMapSize = 4
        val spaceForChildren = value.map { (k, v) ->
            {{ key_type|allocation_size_fn }}(k) +
            {{ value_type|allocation_size_fn }}(v)
        }.sum()
        return spaceForMapSize + spaceForChildren
    }

    override fun write(value: Map<{{ key_type_name }}, {{ value_type_name }}>, buf: Buffer) {
        buf.writeInt(value.size)
        value.forEach { (k, v) ->
            {{ key_type|write_fn }}(k, buf)
            {{ value_type|write_fn }}(v, buf)
        }
    }
}
