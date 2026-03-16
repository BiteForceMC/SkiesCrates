package com.dawnshade.biteforce.bitecrates.data

import net.minecraft.core.BlockPos

class DimensionalBlockPos(
    val dimension: String,
    val x: Int,
    val y: Int,
    val z: Int,
) {
    fun getBlockPos(): BlockPos {
        return BlockPos(x, y, z)
    }

    override fun toString(): String {
        return "DimensionalBlockPos(dimension='$dimension', x=$x, y=$y, z=$z)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DimensionalBlockPos

        if (dimension != other.dimension) return false
        if (x != other.x) return false
        if (y != other.y) return false
        if (z != other.z) return false

        return true
    }

    override fun hashCode(): Int {
        var result = dimension.hashCode()
        result = 31 * result + x
        result = 31 * result + y
        result = 31 * result + z
        return result
    }
}
