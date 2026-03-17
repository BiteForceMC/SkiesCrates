package com.dawnshade.biteforce.bitecrates.config

import com.google.gson.annotations.SerializedName
import com.dawnshade.biteforce.bitecrates.storage.StorageType

class BiteCratesConfig(
    var debug: Boolean = false,
    @SerializedName("interaction_limiter")
    var interactionLimiter: Long = 1000,
    @SerializedName("max_openers_per_crate")
    var maxOpenersPerCrate: Int = 1,
    var storage: Storage = Storage(),
    var keys: KeyOptions = KeyOptions(),
    var migration: MigrationOptions = MigrationOptions(),
    val webhooks: WebhookSettings = WebhookSettings(),
) {
    class Storage(
        val type: StorageType = StorageType.SQLITE,
        val host: String = "",
        val port: Int = 3306,
        val database: String = "BiteCrates",
        val username: String = "root",
        val password: String = "",
        @SerializedName("table_prefix")
        val tablePrefix: String = "bitecrates_",
        val properties: Map<String, String> = mapOf("useUnicode" to "true", "characterEncoding" to "utf8"),
        @SerializedName("pool_settings")
        val poolSettings: StoragePoolSettings = StoragePoolSettings(),
        @SerializedName("url_override")
        val urlOverride: String = ""
    ) {
        override fun toString(): String {
            return "Storage(type=$type, host='$host', port=$port, database='$database', username='$username', " +
                    "password='$password', tablePrefix='$tablePrefix', properties=$properties, " +
                    "poolSettings=$poolSettings, urlOverride='$urlOverride')"
        }
    }

    class StoragePoolSettings(
        @SerializedName("maximum_pool_size")
        val maximumPoolSize: Int = 10,
        @SerializedName("minimum_idle")
        val minimumIdle: Int = 10,
        @SerializedName("keepalive_time")
        val keepaliveTime: Long = 0,
        @SerializedName("connection_timeout")
        val connectionTimeout: Long = 30000,
        @SerializedName("idle_timeout")
        val idleTimeout: Long = 600000,
        @SerializedName("max_lifetime")
        val maxLifetime: Long = 1800000
    ) {
        override fun toString(): String {
            return "StoragePoolSettings(maximumPoolSize=$maximumPoolSize, minimumIdle=$minimumIdle," +
                    " keepaliveTime=$keepaliveTime, connectionTimeout=$connectionTimeout," +
                    " idleTimeout=$idleTimeout, maxLifetime=$maxLifetime)"
        }
    }

    override fun toString(): String {
        return "BiteCratesConfig(debug=$debug, interactionLimiter=$interactionLimiter, maxOpenersPerCrate=$maxOpenersPerCrate, storage=$storage, keys=$keys, migration=$migration, webhooks=$webhooks)"
    }
}
