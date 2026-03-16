package com.dawnshade.biteforce.bitecrates.integrations.bil

import de.tomalbrc.bil.core.holder.positioned.PositionedHolder
import de.tomalbrc.bil.core.holder.wrapper.DisplayWrapper
import de.tomalbrc.bil.core.model.Model
import de.tomalbrc.bil.core.model.Pose
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.phys.Vec3

class CrateModelHolder(
    level: ServerLevel,
    pos: Vec3,
    model: Model,
    val rot: Float? = null,
    val offset: Vec3? = null,
): PositionedHolder(level, pos, model) {
    override fun applyPose(pose: Pose, display: DisplayWrapper<*>) {
        super.applyPose(pose, display)
        if (rot != null) display.element().yaw = rot
        if (offset != null) display.element().offset = offset
    }
}