package com.dawnshade.biteforce.bitecrates.config

import com.google.gson.JsonElement
import com.dawnshade.biteforce.bitecrates.core.BiteCrates
import com.dawnshade.biteforce.bitecrates.util.Utils
import java.io.File
import java.io.FileReader
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.javaType

object Lang {
    
    var KEY_GIVE = listOf("<green>You have received x%amount% %key_name% crate key(s)!")
    var KEY_TAKE = listOf("<green>You had x%amount% %key_name% crate key(s) taken from you!")
    var KEY_SET = listOf("<green>Your %key_name% crate keys were set to x%amount%!")
    var KEY_DUPLICATE_ALERT = listOf("<red>Your inventory contained keys that were marked as duplicated. This incident has been reported...")

    
    var CRATE_GIVE = listOf("<green>You have received a %crate_name% crate!")
    var CRATE_OPENING = listOf("<green>Opening crate...")
    var CRATE_REWARD = listOf("<green>You have received %reward_name% reward from the %crate_name% crate!")
    var CRATE_REWARD_BROADCAST = listOf("<green>%player% has received %reward_name% reward from the %crate_name% crate!")

    
    var ERROR_ALREADY_OPENING = listOf("<red>You are already opening a crate!")
    var ERROR_CRATE_IN_USE = listOf("<red>This crate is currently in use. Please wait for the current opening to finish.")
    var ERROR_NO_PERMISSION = listOf("<red>You don't have permission to open this crate!")
    var ERROR_INVENTORY_SPACE = listOf("<red>You don't have at least %crate_inventory_space% free spaces in your inventory!")
    var ERROR_COST = listOf("<red>You don't have enough money to open this crate!")
    var ERROR_COOLDOWN = listOf("<red>You are on cooldown for this crate! Please wait %cooldown% before opening it again.")
    var ERROR_MISSING_KEYS = listOf("<red>You don't have enough keys to open this crate! Keys: %crate_keys%")

    
    var ERROR_NO_CRATE = listOf("<red>You do not have this crate in your inventory! Not sure how that happened...")
    var ERROR_BALANCE_CHANGED = listOf("<red>Your balance changed while attempting to open this crate! Not sure how that happened...")
    var ERROR_KEYS_CHANGED = listOf("<red>You don't have the required key in your inventory anymore! Not sure how that happened...")

    
    var ERROR_INVALID_PREVIEW = listOf("<red>This crate has an invalid preview! Please contact an admin.")
    var ERROR_INVALID_ANIMATION = listOf("<red>This crate has an invalid animation! Please contact an admin.")
    var ERROR_NO_REWARDS = listOf("<red>This crate has no rewards! Please contact an admin.")
    var ERROR_ECONOMY_PROVIDER = listOf("<red>This crate has an invalid economy provider! Please contact an admin.")
    var ERROR_KEY_NOT_FOUND = listOf("<red>The specified keys for this crate were invalid! Please contact an admin.")
    var ERROR_STORAGE = listOf("<red>There was an error with the storage system! Please contact an admin.")

    @OptIn(ExperimentalStdlibApi::class)
    fun init() {
        
        val defaultMessages = mutableMapOf<String, JsonElement>()
        this::class.memberProperties.forEach { prop ->
            val value = prop.getter.call(this)
            defaultMessages[prop.name] = BiteCrates.Companion.INSTANCE.gsonPretty.toJsonTree(value)
        }

        val langFile = File(BiteCrates.Companion.INSTANCE.configDir, "lang.json")
        if (!langFile.exists()) {
            langFile.parentFile.mkdirs()
            langFile.writeText(BiteCrates.Companion.INSTANCE.gsonPretty.toJson(defaultMessages))
        } else {
            
            val json = BiteCrates.Companion.INSTANCE.gsonPretty.fromJson(FileReader(langFile), JsonElement::class.java).asJsonObject
            defaultMessages.forEach { (key, value) ->
                if (!json.has(key)) {
                    json.add(key, value)
                }
            }
            langFile.writeText(BiteCrates.Companion.INSTANCE.gsonPretty.toJson(json))
        }

        try {
            val json = BiteCrates.Companion.INSTANCE.gsonPretty.fromJson(FileReader(langFile), JsonElement::class.java).asJsonObject

            
            this::class.memberProperties.forEach { prop ->
                
                @Suppress("UNCHECKED_CAST")
                val property = prop as KMutableProperty1<Lang, Any>
                try {
                    
                    json.get(prop.name)?.let {
                        property.set(this, BiteCrates.Companion.INSTANCE.gsonPretty.fromJson(it, prop.returnType.javaType))
                    }
                } catch (e: Exception) {
                    Utils.printError("Failed to load Language setting for ${prop.name}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Utils.printError("Failed to load language file 'lang.json': ${e.message}")
        }
    }
}
