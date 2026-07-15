package com.bcdimtrees.bcdimtrees;

import com.ferreusveritas.dynamictrees.tree.species.Species;
import com.ferreusveritas.dynamictrees.util.LevelContext;
import com.ferreusveritas.dynamictrees.util.SafeChunkBounds;
import com.ferreusveritas.dynamictrees.worldgen.GenerationContext;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;

public final class SpeciesTreeFeature extends Feature<SpeciesTreeFeature.Config> {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final AtomicInteger DEBUG_LOGS = new AtomicInteger();

    public SpeciesTreeFeature() {
        super(Config.CODEC);
    }

    @Override
    public boolean place(final FeaturePlaceContext<Config> context) {
        final Species species = context.config().species();
        if (!species.isValid()) {
            debug("invalid species {}", species.getRegistryName());
            return false;
        }

        final WorldGenLevel level = context.level();
        final BlockPos rootPos = findRootPos(level, context.origin(), species);
        if (rootPos == null) {
            debug("no acceptable soil for species={} origin={} originState={}",
                    species.getRegistryName(), context.origin(), level.getBlockState(context.origin()));
            return false;
        }

        final BlockState rootState = level.getBlockState(rootPos);
        if (!species.isAcceptableSoilForWorldgen(level, rootPos, rootState)) {
            debug("root rejected species={} root={} rootState={}", species.getRegistryName(), rootPos, rootState);
            return false;
        }

        final RandomSource random = context.random();
        final LevelContext levelContext = LevelContext.create(level);
        final GenerationContext generationContext = new GenerationContext(
                levelContext,
                species,
                rootPos,
                rootPos.mutable(),
                level.getBiome(rootPos),
                Direction.Plane.HORIZONTAL.getRandomDirection(random),
                context.config().radius(),
                SafeChunkBounds.ANY_WG
        );
        final boolean generated = species.generate(generationContext);
        debug("generate species={} origin={} root={} rootState={} biome={} generated={}",
                species.getRegistryName(),
                context.origin(),
                rootPos,
                rootState,
                level.getBiome(rootPos).unwrapKey().map(key -> key.location().toString()).orElse("<direct>"),
                generated);
        return generated;
    }

    private static BlockPos findRootPos(final WorldGenLevel level, final BlockPos origin, final Species species) {
        BlockPos pos = origin;
        for (int offset = 0; offset <= 12; offset++) {
            final BlockState state = level.getBlockState(pos);
            if (species.isAcceptableSoilForWorldgen(level, pos, state)) {
                return pos;
            }
            final BlockPos below = pos.below();
            final BlockState belowState = level.getBlockState(below);
            if (species.isAcceptableSoilForWorldgen(level, below, belowState)) {
                return below;
            }
            pos = below;
        }
        return null;
    }

    private static void debug(final String message, final Object... args) {
        if (DEBUG_LOGS.getAndIncrement() < 40) {
            LOGGER.info("[bcdimtrees species_tree] " + message, args);
        }
    }

    public record Config(Species species, int radius) implements FeatureConfiguration {
        public static final Codec<Config> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Species.CODEC.fieldOf("species").forGetter(Config::species),
                Codec.INT.optionalFieldOf("radius", 8).forGetter(Config::radius)
        ).apply(instance, Config::new));
    }
}
