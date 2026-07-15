package com.bcdimtrees.bcdimtrees;

import com.ferreusveritas.dynamictrees.block.rooty.RootyBlock;
import com.ferreusveritas.dynamictrees.block.rooty.SoilHelper;
import com.ferreusveritas.dynamictrees.block.rooty.SoilProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

public final class BcDimTreesSoils {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String[] DIRT_LIKE_SOILS = {
            "undergarden:deepturf_block",
            "undergarden:frozen_deepturf_block",
            "undergarden:ashen_deepturf_block",
            "undergarden:deepsoil",
            "undergarden:coarse_deepsoil",
            "undergarden:deepsoil_farmland",
            "the_finley_dimension_remastered:finley_grass_block",
            "the_finley_dimension_remastered:finley_dirt",
            "the_finley_dimension_remastered:living_grass_block",
            "the_finley_dimension_remastered:living_dirt",
            "callfromthedepth_:lostsoulssoil",
            "callfromthedepth_:lostsoulssand",
            "callfromthedepth_:deepgrass",
            "callfromthedepth_:depthsforestgrass",
            "callfromthedepth_:sporedheatedstone"
    };

    private BcDimTreesSoils() {
    }

    public static void registerDimensionSoils() {
        final Optional<RootyBlock> rootyDirt = SoilHelper.getProperties(Blocks.DIRT).getBlock();
        if (rootyDirt.isEmpty()) {
            LOGGER.error("Could not register dimension Dynamic Trees soils: rooty dirt is unavailable");
            return;
        }

        int registered = 0;
        for (String idText : DIRT_LIKE_SOILS) {
            final ResourceLocation blockId = new ResourceLocation(idText);
            final Block block = ForgeRegistries.BLOCKS.getValue(blockId);
            if (block == null) {
                continue;
            }

            final ResourceLocation soilId = BcDimTrees.location(blockId.getNamespace() + "_" + blockId.getPath());
            final SoilProperties properties = new SoilProperties(block, soilId)
                    .setSoilFlags(SoilHelper.getSoilFlags(SoilHelper.DIRT_LIKE));
            properties.setBlock(rootyDirt.get());
            SoilHelper.addSoilPropertiesToMap(properties);
            registered++;
        }

        LOGGER.info("Registered {} dimension surface blocks as Dynamic Trees dirt-like soil aliases", registered);
    }
}
