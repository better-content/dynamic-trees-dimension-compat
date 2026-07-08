package com.btmdimtrees.btmdimtrees;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public record DimensionTreeBiomeModifier(
        HolderSet<Biome> biomes,
        HolderSet<PlacedFeature> features,
        GenerationStep.Decoration step
) implements BiomeModifier {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final AtomicInteger MODIFY_LOGS = new AtomicInteger();
    private static final Set<String> DIMENSION_FOREST_BIOMES = Set.of(
            "undergarden:dense_forest",
            "undergarden:gronglegrowth",
            "undergarden:smogstem_forest",
            "undergarden:wigglewood_forest",
            "the_finley_dimension_remastered:finley_forest",
            "the_finley_dimension_remastered:living_forest",
            "callfromthedepth_:deepforest",
            "callfromthedepth_:forgottenforest"
    );

    public static final Codec<DimensionTreeBiomeModifier> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(DimensionTreeBiomeModifier::biomes),
            PlacedFeature.LIST_CODEC.fieldOf("features").forGetter(DimensionTreeBiomeModifier::features),
            GenerationStep.Decoration.CODEC.fieldOf("step").forGetter(DimensionTreeBiomeModifier::step)
    ).apply(instance, DimensionTreeBiomeModifier::new));

    public DimensionTreeBiomeModifier {
        LOGGER.info("Decoded Bound to Matter dimension Dynamic Trees biome modifier: step={}", step);
    }

    @Override
    public void modify(final Holder<Biome> biome, final Phase phase, final ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD || !targetsBiome(biome)) {
            return;
        }

        final var featureList = builder.getGenerationSettings().getFeatures(step);
        for (Holder<PlacedFeature> feature : features) {
            if (!featureList.contains(feature)) {
                featureList.add(feature);
                if (MODIFY_LOGS.getAndIncrement() < 20) {
                    LOGGER.info("Added Dynamic Trees placed feature to dimension forest biome {}", biome.unwrapKey()
                            .map(key -> key.location().toString())
                            .orElse("<direct>"));
                }
            }
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return BtmDimTreesBiomeModifiers.DIMENSION_TREE.get();
    }

    private boolean targetsBiome(final Holder<Biome> biome) {
        return biomes.contains(biome) || biome.unwrapKey()
                .map(key -> DIMENSION_FOREST_BIOMES.contains(key.location().toString()))
                .orElse(false);
    }
}
