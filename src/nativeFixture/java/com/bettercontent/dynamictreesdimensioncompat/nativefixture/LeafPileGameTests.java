package com.bettercontent.dynamictreesdimensioncompat.nativefixture;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.ferreusveritas.dynamictrees.block.leaves.LeavesProperties;
import com.bettercontent.dynamictreesdimensioncompat.BcDimTrees;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@Mod("dynamic_trees_dimension_fixture")
@GameTestHolder("dynamic_trees_dimension_fixture")
@PrefixGameTestTemplate(false)
public final class LeafPileGameTests {
    @GameTest(templateNamespace = "dynamic_trees_dimension_fixture", template = "empty")
    public static void generated_leaf_piles_preserve_native_semantics(GameTestHelper helper) throws Exception {
        var level = helper.getLevel();
        var resources = level.getServer().getResourceManager();
        String template;
        try (var reader = resources.getResource(new ResourceLocation("immersive_weathering", "loot_tables/blocks/oak_leaf_pile.json")).orElseThrow().openAsReader()) {
            template = new com.google.gson.Gson().toJson(JsonParser.parseReader(reader));
        }
        int tables = 0, drops = 0;
        for (String wood : List.of("grongle", "smogstem", "wigglewood")) {
            var properties = LeavesProperties.REGISTRY.get(BcDimTrees.location(wood));
            var primitive = properties.getPrimitiveLeavesItemStack();
            helper.assertTrue(!primitive.isEmpty(), "Real primitive leaf item must exist for " + wood);
            var itemId = ForgeRegistries.ITEMS.getKey(primitive.getItem());
            helper.assertTrue(itemId != null && ForgeRegistries.ITEMS.containsKey(itemId), "Primitive item must resolve in native registry");
            helper.assertTrue(!ForgeRegistries.ITEMS.containsKey(new ResourceLocation(BcDimTrees.MODID, wood + "_leaves")),
                    "Fixture must exercise the block-only dynamic leaf identity, not a newly invented item");
            // The ordinary Undergarden type goes through the same transformed IW builder.
            // Its effective generated JSON is the negative control for unrelated leaf types.
            for (String namespace : List.of(BcDimTrees.MODID, "undergarden")) {
                String pilePath = namespace + "/" + wood + "_leaf_pile";
                var pileId = new ResourceLocation("immersive_weathering", pilePath);
                var resource = new ResourceLocation("immersive_weathering", "loot_tables/blocks/" + pilePath + ".json");
                JsonElement actual;
                try (var reader = resources.getResource(resource).orElseThrow().openAsReader()) {
                    actual = JsonParser.parseReader(reader);
                }
                var expected = JsonParser.parseString(template.replace("oak_leaf_pile", pilePath)
                        .replace("minecraft:oak_leaves", itemId.toString()));
                helper.assertTrue(expected.equals(actual), "Effective IW generation must preserve every native count/condition/function: " + pileId);
                verifyItemReferences(helper, actual);
                tables++;
                if (!namespace.equals(BcDimTrees.MODID)) continue;
                helper.assertTrue(ForgeRegistries.BLOCKS.containsKey(pileId), "Native dynamic leaf-pile block must exist: " + pileId);
                var block = ForgeRegistries.BLOCKS.getValue(pileId);
                var layers = block.getStateDefinition().getProperty("layers");
                helper.assertTrue(layers instanceof IntegerProperty, "Native pile must expose integer layers");
                var layerProperty = (IntegerProperty) layers;
                var table = level.getServer().getLootData().getLootTable(block.getLootTable());
                helper.assertTrue(table != LootTable.EMPTY, "Native parsed table must not fall back to EMPTY");
                var silk = new ItemStack(Items.DIAMOND_AXE);
                silk.enchant(Enchantments.SILK_TOUCH, 1);
                for (var tool : List.of(new ItemStack(Items.SHEARS), silk)) {
                    for (int layer = 0; layer <= 8; layer++) {
                        var state = block.defaultBlockState().setValue(layerProperty, layer);
                        var params = new LootParams.Builder(level)
                                .withParameter(LootContextParams.BLOCK_STATE, state)
                                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(net.minecraft.core.BlockPos.ZERO)))
                                .withParameter(LootContextParams.TOOL, tool).create(LootContextParamSets.BLOCK);
                        var output = table.getRandomItems(params);
                        var expectedItem = layer == 8 ? primitive.getItem() : block.asItem();
                        int expectedCount = layer < 2 ? 1 : layer;
                        if (layer == 8) expectedCount = 1;
                        helper.assertTrue(output.size() == 1 && output.get(0).is(expectedItem) && output.get(0).getCount() == expectedCount,
                                "Native layer/tool loot mismatch: " + pileId + " layer=" + layer + " output=" + output);
                        drops++;
                    }
                }
            }
        }
        var report = new com.google.gson.JsonObject();
        report.addProperty("run_token", System.getProperty("bc.dtdim.runToken"));
        report.addProperty("id", "generated_leaf_piles_preserve_native_semantics");
        report.addProperty("passed", true);
        report.addProperty("effective_tables", tables);
        report.addProperty("native_drop_cases", drops);
        Files.writeString(Path.of(System.getProperty("bc.dtdim.report")), report.toString());
        helper.succeed();
    }

    private static void verifyItemReferences(GameTestHelper helper, JsonElement value) {
        if (value.isJsonArray()) value.getAsJsonArray().forEach(child -> verifyItemReferences(helper, child));
        else if (value.isJsonObject()) {
            var object = value.getAsJsonObject();
            if (object.has("type") && "minecraft:item".equals(object.get("type").getAsString())) {
                var id = new ResourceLocation(object.get("name").getAsString());
                helper.assertTrue(ForgeRegistries.ITEMS.containsKey(id) && ForgeRegistries.ITEMS.getValue(id) != Items.AIR,
                        "Effective loot item must actually exist: " + id);
            }
            object.entrySet().forEach(entry -> verifyItemReferences(helper, entry.getValue()));
        }
    }
}
