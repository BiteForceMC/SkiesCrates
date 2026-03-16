package com.dawnshade.biteforce.bitecrates.util

import com.dawnshade.biteforce.bitecrates.data.DimensionalBlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.chunk.LevelChunk

data class ChunkKey(
    val dimension: String,
    val x: Int,
    val z: Int
) {
    companion object {
        fun of(pos: DimensionalBlockPos): ChunkKey {
            val cx = pos.x shr 4
            val cz = pos.z shr 4
            return ChunkKey(pos.dimension, cx, cz)
        }

        fun of(level: ServerLevel, chunk: LevelChunk): ChunkKey {
            return ChunkKey(level.dimension().location().toString(), chunk.pos.x, chunk.pos.z)
        }
    }
}
