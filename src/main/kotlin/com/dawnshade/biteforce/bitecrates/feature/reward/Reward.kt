package com.dawnshade.biteforce.bitecrates.feature.reward

import com.google.gson.*
import com.dawnshade.biteforce.bitecrates.core.BiteCrates
import com.dawnshade.biteforce.bitecrates.config.Lang
import com.dawnshade.biteforce.bitecrates.config.item.GenericItem
import com.dawnshade.biteforce.bitecrates.data.Crate
import com.dawnshade.biteforce.bitecrates.state.userdata.CrateData
import com.dawnshade.biteforce.bitecrates.state.userdata.UserData
import com.dawnshade.biteforce.bitecrates.util.TextUtils
import com.dawnshade.biteforce.bitecrates.util.Utils
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.item.ItemStack
import java.lang.reflect.Type
import java.util.*

abstract class Reward(
    val type: RewardType = RewardType.COMMAND_PLAYER,
    val name: String = "",
    val display: GenericItem? = null,
    val weight: Int = 1,
    val limits: RewardLimits? = null,
    val broadcast: Boolean = false,
    val preview: GenericItem? = null
) {
    lateinit var id: String

    open fun giveReward(player: ServerPlayer, crate: Crate): Boolean {
        Utils.printDebug("Attempting to execute a ${type.identifier} reward: $this")

        Lang.CRATE_REWARD.forEach {
            player.sendMessage(TextUtils.parseAllNative(
                player,
                crate.parsePlaceholders(it)
                    .replace("%reward_name%", name)
            ))
        }

        if (broadcast) {
            Lang.CRATE_REWARD_BROADCAST.forEach {
                BiteCrates.INSTANCE.adventure.all().sendMessage(TextUtils.parseAllNative(
                    player,
                    crate.parsePlaceholders(it)
                        .replace("%reward_name%", name)
                ))
            }
        }
        return true
    }

    abstract fun getGenericDisplay(): GenericItem
    open fun getDisplayItem(player: ServerPlayer, placeholders: Map<String, String> = emptyMap()): ItemStack {
        return getGenericDisplay().also {
            if (it.name == null) it.name = name
        }.createItemStack(player, placeholders)
    }

    fun canReceive(userData: UserData, crate: Crate): Boolean {
        if (limits == null) return true

        val playerLimits = limits.player
        if (playerLimits != null) {
            if (playerLimits.amount >= 0) {
                val limitData = userData.crates.getOrDefault(crate.id, CrateData()).rewards?.get(id)
                if (limitData != null && limitData.claimed >= playerLimits.amount) {
                    if (playerLimits.cooldown <= 0 || (limitData.time + (playerLimits.cooldown * 1000)) > System.currentTimeMillis()) {
                        return false
                    }
                }
            }
        }

        return true
    }

    fun getPlayerLimit(): Int {
        return limits?.player?.amount ?: 0
    }

    fun getPlaceholders(userData: UserData, crate: Crate): Map<String, String> {
        return mapOf(
            "%reward_name%" to (preview?.name ?: name),
            "%reward_display_name%" to (getGenericDisplay().name ?: ""),
            "%reward_display_lore%" to (getGenericDisplay().lore?.joinToString("\n") ?: ""),
            "%reward_id%" to id,
            "%reward_weight%" to weight.toString(),
            "%reward_percent%" to String.format(Locale.US, "%.2f", calculatePercent(this, crate)),
            "%reward_limit_player%" to (limits?.player?.amount?.toString() ?: "0"),
            "%reward_limit_player_claimed%" to userData.getRewardLimits(crate, this).toString()
        )
    }

    private fun calculatePercent(reward: Reward, crate: Crate): Double {
        return (reward.weight.toDouble() / crate.rewards.values.sumOf { it.weight }) * 100
    }

    override fun toString(): String {
        return "Reward(type=$type, name='$name', display=$display, weight=$weight, limits=$limits, broadcast=$broadcast)"
    }

    internal class Adapter : JsonSerializer<Reward>, JsonDeserializer<Reward> {
        override fun serialize(src: Reward, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return context.serialize(src, src::class.java)
        }

        override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Reward {
            val jsonObject: JsonObject = json.getAsJsonObject()
            val value = jsonObject.get("type").asString
            val type: RewardType? = RewardType.valueOfAnyCase(value)
            return try {
                context.deserialize(json, type!!.clazz)
            } catch (e: NullPointerException) {
                throw JsonParseException("Could not deserialize reward type: $value", e)
            }
        }
    }

    class RewardMapAdapter: JsonSerializer<MutableMap<String, Reward>>, JsonDeserializer<MutableMap<String, Reward>> {
        override fun serialize(
            src: MutableMap<String, Reward>,
            typeOfSrc: Type,
            context: JsonSerializationContext
        ): JsonElement {
            val obj = JsonObject()
            for ((key, reward) in src) {
                obj.add(key, context.serialize(reward))
            }
            return obj
        }

        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): MutableMap<String, Reward> {
            val map = mutableMapOf<String, Reward>()
            val obj = json.asJsonObject
            for ((key, value) in obj.entrySet()) {
                val reward = context.deserialize<Reward>(value, Reward::class.java)
                reward.id = key
                map[key] = reward
            }
            return map
        }
    }
}
