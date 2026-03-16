package com.dawnshade.biteforce.bitecrates.integrations.holodisplays

import com.dawnshade.biteforce.bitecrates.integrations.IntegratedMod
import com.dawnshade.biteforce.bitecrates.core.HologramsManager
import com.dawnshade.biteforce.bitecrates.util.Utils

class HoloDisplaysIntegration: IntegratedMod {
    override fun onServerStarted() {
        Utils.printInfo("The mod HoloDisplays was found, enabling integrations...")
        HologramsManager.load()
    }

    override fun onServerShutdown() {
        Utils.printInfo("Shutting down HoloDisplays integrations...")
        HologramsManager.unload()
    }
}