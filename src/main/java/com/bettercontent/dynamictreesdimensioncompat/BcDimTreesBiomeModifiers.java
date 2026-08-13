package com.bettercontent.dynamictreesdimensioncompat;

import com.mojang.serialization.Codec;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class BcDimTreesBiomeModifiers {
    public static final DeferredRegister<Codec<? extends BiomeModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, BcDimTrees.MODID);

    public static final RegistryObject<Codec<DimensionTreeBiomeModifier>> DIMENSION_TREE =
            SERIALIZERS.register("dimension_tree", () -> DimensionTreeBiomeModifier.CODEC);

    private BcDimTreesBiomeModifiers() {
    }
}
