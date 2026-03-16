package com.dawnshade.biteforce.bitecrates.storage.file

import com.dawnshade.biteforce.bitecrates.core.BiteCrates
import com.dawnshade.biteforce.bitecrates.config.ConfigManager
import com.dawnshade.biteforce.bitecrates.state.userdata.UsedKeyData
import com.dawnshade.biteforce.bitecrates.state.userdata.UserData
import com.dawnshade.biteforce.bitecrates.storage.IStorage
import java.util.*
import java.util.concurrent.CompletableFuture

class FileStorage : IStorage {
    private var fileData: FileData = ConfigManager.loadFile(STORAGE_FILENAME, FileData(), "", true)

    companion object {
        private const val STORAGE_FILENAME = "storage.json"
    }

    override fun getUser(uuid: UUID): UserData {
        val userData = fileData.userdata[uuid]
        return userData ?: UserData(uuid)
    }

    override fun saveUser(userData: UserData): Boolean {
        fileData.userdata[userData.uuid] = userData
        val snapshot = HashMap(fileData.userdata)
        val fileDataCopy = FileData().apply { userdata = snapshot }
        return ConfigManager.saveFile(STORAGE_FILENAME, fileDataCopy)
    }

    override fun getUsedKey(uuid: UUID): UsedKeyData? {
        return fileData.usedKeys[uuid]
    }

    override fun saveUsedKey(usedKeyData: UsedKeyData): Boolean {
        fileData.usedKeys[usedKeyData.uuid] = usedKeyData
        val snapshot = HashMap(fileData.usedKeys)
        val fileDataCopy = FileData().apply { usedKeys = snapshot }
        return ConfigManager.saveFile(STORAGE_FILENAME, fileDataCopy)
    }

    override fun getUserAsync(uuid: UUID): CompletableFuture<UserData> {
        return CompletableFuture.supplyAsync({
            getUser(uuid)
        }, BiteCrates.INSTANCE.asyncExecutor)
    }

    override fun saveUserAsync(userData: UserData): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync({
            saveUser(userData)
        }, BiteCrates.INSTANCE.asyncExecutor)
    }

    override fun getUsedKeyAsync(uuid: UUID): CompletableFuture<UsedKeyData?> {
        return CompletableFuture.supplyAsync({
            getUsedKey(uuid)
        }, BiteCrates.INSTANCE.asyncExecutor)
    }

    override fun saveUsedKeyAsync(usedKeyData: UsedKeyData): CompletableFuture<Boolean> {
        return CompletableFuture.supplyAsync({
            saveUsedKey(usedKeyData)
        }, BiteCrates.INSTANCE.asyncExecutor)
    }
}
