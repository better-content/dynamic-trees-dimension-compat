package com.bettercontent.dynamictreesdimensioncompat.compat;

import com.bettercontent.dynamictreesdimensioncompat.BcDimTrees;
import com.ferreusveritas.dynamictrees.block.leaves.LeavesProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;

/** IW's leaf-pile builder needs an item ID, not a Dynamic Trees block-only ID. */
public final class ImmersiveWeatheringLeafItems {
    private ImmersiveWeatheringLeafItems() {}

    static String propertiesName(String leavesBlock) {
        return switch (leavesBlock) {
            case "dynamic_trees_dimension_compat:grongle_leaves" -> "grongle";
            case "dynamic_trees_dimension_compat:smogstem_leaves" -> "smogstem";
            case "dynamic_trees_dimension_compat:wigglewood_leaves" -> "wigglewood";
            default -> null;
        };
    }

    public static String itemForLeaves(String leavesBlock) {
        String name = propertiesName(leavesBlock);
        if (name == null) return leavesBlock;
        var primitive = LeavesProperties.REGISTRY.get(BcDimTrees.location(name)).getPrimitiveLeavesItemStack();
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(primitive.getItem());
        if (primitive.isEmpty() || itemId == null) {
            throw new IllegalStateException("Missing primitive leaf item for " + leavesBlock);
        }
        return itemId.toString();
    }
}
