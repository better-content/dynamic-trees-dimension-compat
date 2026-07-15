package com.bcdimtrees.bcdimtrees;

import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BcDimTreesFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, BcDimTrees.MODID);

    public static final RegistryObject<Feature<SpeciesTreeFeature.Config>> SPECIES_TREE =
            FEATURES.register("species_tree", SpeciesTreeFeature::new);

    private BcDimTreesFeatures() {
    }
}
