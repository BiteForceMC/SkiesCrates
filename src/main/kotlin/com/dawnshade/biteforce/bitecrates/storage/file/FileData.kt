package com.dawnshade.biteforce.bitecrates.storage.file

import com.google.gson.annotations.SerializedName
import com.dawnshade.biteforce.bitecrates.state.userdata.UsedKeyData
import com.dawnshade.biteforce.bitecrates.state.userdata.UserData
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class FileData {
    var userdata: MutableMap<UUID, UserData> = ConcurrentHashMap()
    @SerializedName("used_keys")
    var usedKeys: MutableMap<UUID, UsedKeyData> = ConcurrentHashMap()

    override fun toString(): String {
        return "FileData(userdata=$userdata, usedKeys=$usedKeys)"
    }
}
