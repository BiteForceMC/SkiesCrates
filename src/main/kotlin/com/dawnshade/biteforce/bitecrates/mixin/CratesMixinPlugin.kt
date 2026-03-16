package com.dawnshade.biteforce.bitecrates.mixin

import net.fabricmc.loader.api.FabricLoader
import org.objectweb.asm.tree.ClassNode
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin
import org.spongepowered.asm.mixin.extensibility.IMixinInfo

class CratesMixinPlugin : IMixinConfigPlugin {
    override fun onLoad(mixinPackage: String?) {}
    override fun getRefMapperConfig(): String? = null

    override fun shouldApplyMixin(targetClassName: String?, mixinClassName: String?): Boolean {
        return !(mixinClassName == "com.dawnshade.biteforce.bitecrates.mixin.ViewerHandlerMixin" && !FabricLoader.getInstance().isModLoaded("holodisplays"))
    }

    override fun acceptTargets(p0: Set<String?>?, p1: Set<String?>?) {}
    override fun getMixins(): List<String?>? = null
    override fun preApply(p0: String?, p1: ClassNode?, p2: String?, p3: IMixinInfo?) {}
    override fun postApply(p0: String?, p1: ClassNode?, p2: String?, p3: IMixinInfo?) {}
}