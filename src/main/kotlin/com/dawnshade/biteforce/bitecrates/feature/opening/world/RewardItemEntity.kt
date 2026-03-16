package com.dawnshade.biteforce.bitecrates.feature.opening.world

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3

class RewardItemEntity(
    level: ServerLevel,
    pos: Vec3,
    itemStack: ItemStack,
): ItemEntity(level, pos.x, pos.y, pos.z, itemStack) {
    init {
        deltaMovement = Vec3.ZERO
        isNoGravity = true
        isInvulnerable = true

        setPickUpDelay(Int.MAX_VALUE)
    }

    override fun shouldBeSaved(): Boolean = false
}