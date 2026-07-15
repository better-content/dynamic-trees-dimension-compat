package com.bcdimtrees.bcdimtrees;

import com.ferreusveritas.dynamictrees.api.worldgen.BiomePropertySelectors;
import com.ferreusveritas.dynamictrees.tree.family.Family;
import com.ferreusveritas.dynamictrees.tree.species.Species;
import com.ferreusveritas.dynamictrees.worldgen.BiomeDatabase;
import com.ferreusveritas.dynamictrees.worldgen.BiomeDatabases;
import com.mojang.brigadier.Command;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Optional;

@Mod.EventBusSubscriber(modid = BcDimTrees.MODID)
public final class BcDimTreesCommands {
    private BcDimTreesCommands() {
    }

    @SubscribeEvent
    public static void register(final RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("bcdimtrees")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("resource_status")
                        .executes(ctx -> resourceStatus(
                                line -> ctx.getSource().sendSuccess(() -> Component.literal(line), false)
                        )))
                .then(Commands.literal("debug_biome")
                        .then(Commands.argument("dimension", ResourceLocationArgument.id())
                                .then(Commands.argument("biome", ResourceLocationArgument.id())
                                        .executes(ctx -> debugBiome(
                                                ctx.getSource().getServer(),
                                                ResourceLocationArgument.getId(ctx, "dimension"),
                                                ResourceLocationArgument.getId(ctx, "biome"),
                                                line -> ctx.getSource().sendSuccess(() -> Component.literal(line), false)
                                        ))))));
    }

    private static int resourceStatus(final java.util.function.Consumer<String> output) {
        for (String tree : List.of(
                "grongle",
                "smogstem",
                "wigglewood",
                "finley_wood",
                "living_wood",
                "silent_tree"
        )) {
            final ResourceLocation id = BcDimTrees.location(tree);
            final Species species = Species.REGISTRY.get(id);
            final Family family = Family.REGISTRY.get(id);
            output.accept(id
                    + " speciesValid=" + species.isValid()
                    + " familyValid=" + family.isValid()
                    + " branch=" + family.getBranch().map(block -> ForgeRegistries.BLOCKS.getKey(block).toString()).orElse("<missing>")
                    + " strippedBranch=" + family.getStrippedBranch().map(block -> ForgeRegistries.BLOCKS.getKey(block).toString()).orElse("<missing>")
                    + " leavesBlock=" + species.getLeavesBlock().map(block -> ForgeRegistries.BLOCKS.getKey(block).toString()).orElse("<missing>")
                    + " primitiveLeaves=" + species.getPrimitiveLeaves().map(block -> ForgeRegistries.BLOCKS.getKey(block).toString()).orElse("<missing>")
                    + " seed=" + species.getSeed().map(item -> ForgeRegistries.ITEMS.getKey(item).toString()).orElse("<missing>")
                    + " sapling=" + species.getSapling().map(block -> ForgeRegistries.BLOCKS.getKey(block).toString()).orElse("<missing>"));
        }
        return Command.SINGLE_SUCCESS;
    }

    private static int debugBiome(final MinecraftServer server, final ResourceLocation dimensionId, final ResourceLocation biomeId,
                                  final java.util.function.Consumer<String> output) {
        final ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
        final ServerLevel level = server.getLevel(dimensionKey);
        if (level == null) {
            output.accept("dimension not loaded: " + dimensionId);
            return 0;
        }

        final ResourceKey<Biome> biomeKey = ResourceKey.create(Registries.BIOME, biomeId);
        final Optional<Holder.Reference<Biome>> biome = server.registryAccess()
                .registryOrThrow(Registries.BIOME)
                .getHolder(biomeKey);
        if (biome.isEmpty()) {
            output.accept("biome not registered: " + biomeId);
            return 0;
        }

        final BiomeDatabase database = BiomeDatabases.getDimensionalOrDefault(dimensionId);
        final BiomeDatabase.Entry entry = database.getEntry(biome.get());
        final BiomePropertySelectors.SpeciesSelection selection = entry.getSpeciesSelector()
                .getSpecies(BlockPos.ZERO, Blocks.DIRT.defaultBlockState(), RandomSource.create(1L));
        final Species species = selection.getSpecies();
        final BiomePropertySelectors.Chance chance = entry.getChanceSelector()
                .getChance(RandomSource.create(1L), species, 8);
        final boolean dynamicTreeFeaturePresent = biome.get().value().getGenerationSettings().features().stream()
                .flatMap(holderSet -> holderSet.stream())
                .map(Holder::unwrapKey)
                .flatMap(Optional::stream)
                .anyMatch(key -> new ResourceLocation("dynamictrees", "dynamic_tree").equals(key.location()));
        final String soilChecks = String.join(",",
                soilCheck(level, species, "minecraft:dirt")
        );

        output.accept("dimension=" + dimensionId
                + " biome=" + biomeId
                + " databasePopulated=" + database.isPopulated()
                + " blacklisted=" + entry.isBlacklisted()
                + " handled=" + selection.isHandled()
                + " species=" + species.getRegistryName()
                + " speciesValid=" + species.isValid()
                + " chance=" + chance
                + " forestness=" + entry.getForestness()
                + " heightmap=" + entry.getHeightmap()
                + " multipass1=" + entry.getMultipass().apply(1)
                + " dynamicFeaturePresent=" + dynamicTreeFeaturePresent
                + " soilChecks=" + soilChecks);
        return Command.SINGLE_SUCCESS;
    }

    private static String soilCheck(final ServerLevel level, final Species species, final String blockId) {
        final ResourceLocation id = new ResourceLocation(blockId);
        final BlockState state = ForgeRegistries.BLOCKS.getValue(id).defaultBlockState();
        return blockId + "=" + species.isAcceptableSoilForWorldgen(level, BlockPos.ZERO, state);
    }
}
