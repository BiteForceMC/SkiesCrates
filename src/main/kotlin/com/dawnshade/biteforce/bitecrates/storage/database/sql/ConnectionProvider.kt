package com.dawnshade.biteforce.bitecrates.storage.database.sql

import java.sql.Connection
import java.sql.SQLException


interface ConnectionProvider {
    @Throws(SQLException::class)
    fun init()
    @Throws(SQLException::class)
    fun shutdown()
    @Throws(SQLException::class)
    fun createConnection(): Connection
    fun getName(): String
    fun isInitialized(): Boolean
}
