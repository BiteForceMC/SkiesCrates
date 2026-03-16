package com.dawnshade.biteforce.bitecrates.storage

import com.dawnshade.biteforce.bitecrates.config.BiteCratesConfig
import com.dawnshade.biteforce.bitecrates.state.userdata.UsedKeyData
import com.dawnshade.biteforce.bitecrates.state.userdata.UserData
import com.dawnshade.biteforce.bitecrates.storage.database.MongoStorage
import com.dawnshade.biteforce.bitecrates.storage.database.sql.SQLStorage
import com.dawnshade.biteforce.bitecrates.storage.file.FileStorage
import net.minecraft.server.level.ServerPlayer
import java.util.*
import java.util.concurrent.CompletableFuture

interface IStorage {
    companion object {
        fun load(config: BiteCratesConfig.Storage): IStorage {
            return when (config.type) {
                StorageType.JSON -> FileStorage()
                StorageType.MONGO -> MongoStorage(config)
                StorageType.MYSQL, StorageType.SQLITE -> SQLStorage(config)
            }
        }
    }

    fun getUser(uuid: UUID): UserData
    fun getUser(player: ServerPlayer): UserData = getUser(player.uuid)
    fun saveUser(userData: UserData): Boolean
    fun getUsedKey(uuid: UUID): UsedKeyData?
    fun saveUsedKey(usedKeyData: UsedKeyData): Boolean

    fun getUserAsync(uuid: UUID): CompletableFuture<UserData>
    fun getUserAsync(player: ServerPlayer): CompletableFuture<UserData> = getUserAsync(player.uuid)
    fun saveUserAsync(userData: UserData): CompletableFuture<Boolean>
    fun getUsedKeyAsync(uuid: UUID): CompletableFuture<UsedKeyData?>
    fun saveUsedKeyAsync(usedKeyData: UsedKeyData): CompletableFuture<Boolean>

    fun close() {}
}
