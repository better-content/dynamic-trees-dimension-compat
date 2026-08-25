package com.bettercontent.dynamictreesdimensioncompat.mixin;

import com.bettercontent.dynamictreesdimensioncompat.DtaetherTagCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagEntry;
import net.minecraft.util.ExtraCodecs;
import net.minecraftforge.fml.ModList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TagEntry.class, remap = false)
abstract class TagEntryMixin {
    @Shadow(aliases = "f_215913_")
    @Final
    private ResourceLocation id;

    @Shadow(aliases = "f_215915_")
    @Final
    @Mutable
    private boolean required;

    @Inject(
            method = "<init>(Lnet/minecraft/resources/ResourceLocation;ZZ)V",
            at = @At("RETURN"),
            remap = false
    )
    private void betterContent$makeStructuredDtaetherBranchOptional(
            final ResourceLocation id,
            final boolean tag,
            final boolean required,
            final CallbackInfo callback
    ) {
        betterContent$makeDtaetherBranchOptional();
    }

    @Inject(
            method = "<init>(Lnet/minecraft/util/ExtraCodecs$TagOrElementLocation;Z)V",
            at = @At("RETURN"),
            remap = false
    )
    private void betterContent$makeCompactDtaetherBranchOptional(
            final ExtraCodecs.TagOrElementLocation location,
            final boolean required,
            final CallbackInfo callback
    ) {
        betterContent$makeDtaetherBranchOptional();
    }

    private void betterContent$makeDtaetherBranchOptional() {
        if (DtaetherTagCompat.shouldTreatAsOptional(id, ModList.get().isLoaded("dtaether"))) {
            required = false;
        }
    }
}
