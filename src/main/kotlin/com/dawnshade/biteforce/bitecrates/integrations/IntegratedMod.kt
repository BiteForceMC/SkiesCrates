package com.dawnshade.biteforce.bitecrates.integrations

interface IntegratedMod {
    fun onInit() {}
    fun onServerStarted() {}
    fun onServerStarting() {}
    fun onServerShutdown() {}
}