package com.bettercontent.dynamictreesdimensioncompat.mixin;

import com.bettercontent.dynamictreesdimensioncompat.compat.ImmersiveWeatheringLeafItems;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Optional pinned IW 2.0.5 integration; absent target is inert, present target is strict. */
@Pseudo
@Mixin(targets = "com.ordana.immersive_weathering.dynamicpack.ServerDynamicResourcesHandler", remap = false)
public abstract class ImmersiveWeatheringLeafPileMixin {
    @ModifyVariable(method = "addLeafPileJson", at = @At("HEAD"), argsOnly = true,
            ordinal = 1, require = 1, remap = false)
    private String dimensionTrees$primitiveLeafItem(String leavesBlock) {
        return ImmersiveWeatheringLeafItems.itemForLeaves(leavesBlock);
    }
}
