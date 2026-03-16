package com.dawnshade.biteforce.bitecrates.storage.database.sql.providers

import com.dawnshade.biteforce.bitecrates.core.BiteCrates
import com.dawnshade.biteforce.bitecrates.config.BiteCratesConfig
import com.zaxxer.hikari.HikariConfig
import java.io.File

class H2Provider(config: BiteCratesConfig.Storage) : HikariCPProvider(config) {
    override fun getConnectionURL(): String = String.format(
        "jdbc:h2:%s;AUTO_SERVER=TRUE",
        File(BiteCrates.INSTANCE.configDir, "storage.db").toPath().toAbsolutePath()
    )

    override fun getDriverClassName(): String = "org.h2.Driver"
    override fun getDriverName(): String = "h2"
    override fun configure(config: HikariConfig) {}
}
