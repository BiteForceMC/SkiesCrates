package com.dawnshade.biteforce.bitecrates.config.block

import com.dawnshade.biteforce.bitecrates.data.DimensionalBlockPos

class CrateBlockLocation(
    val dimension: String,
    val x: Int,
    val y: Int,
    val z: Int,
    val model: ModelOptions? = null, 
    val hologram: HologramOptions? = null, 
    val previewDisplay: PreviewDisplayOptions? = null,
    val particles: String? = null, 
) {
    fun getDimensionalBlockPos(): DimensionalBlockPos {
        return DimensionalBlockPos(dimension, x, y, z)
    }

    fun equalsDimBlockPos(other: DimensionalBlockPos): Boolean {
        return dimension.equals(other.dimension, true) && x == other.x && y == other.y && z == other.z
    }
}
