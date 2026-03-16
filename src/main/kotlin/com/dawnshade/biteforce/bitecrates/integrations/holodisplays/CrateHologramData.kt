package com.dawnshade.biteforce.bitecrates.integrations.holodisplays

import com.dawnshade.biteforce.bitecrates.data.CrateInstance
import java.util.*

class CrateHologramData(
    val instance: CrateInstance,
    val hiddenPlayers: MutableList<UUID> = mutableListOf()
)