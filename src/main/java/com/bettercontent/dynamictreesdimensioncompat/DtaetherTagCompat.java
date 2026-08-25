package com.bettercontent.dynamictreesdimensioncompat;

import net.minecraft.resources.ResourceLocation;

public final class DtaetherTagCompat {
    static final ResourceLocation OBSOLETE_IMBUED_SKYROOT_BRANCH =
            ResourceLocation.fromNamespaceAndPath("dtaether", "imbued_skyroot_branch");

    private DtaetherTagCompat() {
    }

    public static boolean shouldTreatAsOptional(final ResourceLocation id, final boolean dtaetherLoaded) {
        return dtaetherLoaded && OBSOLETE_IMBUED_SKYROOT_BRANCH.equals(id);
    }
}
