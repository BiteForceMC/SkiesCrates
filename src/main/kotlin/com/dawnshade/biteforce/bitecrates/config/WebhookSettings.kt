package com.dawnshade.biteforce.bitecrates.config

import com.google.gson.annotations.SerializedName

class WebhookSettings(
    @SerializedName("duplicate_key")
    val duplicateKey: DuplicateKeyOptions = DuplicateKeyOptions(),
) {
    class DuplicateKeyOptions(
        val url: String = "",
    ) {
        override fun toString(): String {
            return "DuplicateKeyOptions(url='$url')"
        }
    }

    override fun toString(): String {
        return "WebhookSettings(duplicateKey='$duplicateKey')"
    }
}