package com.dawnshade.biteforce.bitecrates.feature.key

import com.dawnshade.biteforce.bitecrates.config.item.GenericItem

class Key(
    val enabled: Boolean = true,
    val name: String = "",
    val display: GenericItem = GenericItem(),
    val virtual: Boolean = false,
    val unique: Boolean = false,
) {
    
    lateinit var id: String

    override fun toString(): String {
        return "Key(id='$id', enabled=$enabled, name='$name', display=$display, virtual=$virtual, unique=$unique)"
    }
}
