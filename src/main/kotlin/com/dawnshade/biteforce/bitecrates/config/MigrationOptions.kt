package com.dawnshade.biteforce.bitecrates.config

class MigrationOptions(
    val keys: List<KeyInstance>? = null
) {
    class KeyInstance(
        val key: String,
        val nbt: NbtPair
    ) {
        class NbtPair(
            val key: String,
            val value: String
        ) {
            override fun toString(): String {
                return "NbtPair(key='$key', value='$value')"
            }
        }

        override fun toString(): String {
            return "KeyInstance(key='$key', nbt='$nbt')"
        }
    }

    override fun toString(): String {
        return "MigrationOptions(keys=$keys)"
    }
}
