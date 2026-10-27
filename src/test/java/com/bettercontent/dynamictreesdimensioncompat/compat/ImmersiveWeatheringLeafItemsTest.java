package com.bettercontent.dynamictreesdimensioncompat.compat;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class ImmersiveWeatheringLeafItemsTest {
    @Test
    void onlyOwnedLeafBlocksSelectTheirAuthoredPrimitiveProperties() throws Exception {
        for (String wood : new String[]{"grongle", "smogstem", "wigglewood"}) {
            String properties = ImmersiveWeatheringLeafItems.propertiesName("dynamic_trees_dimension_compat:" + wood + "_leaves");
            assertEquals(wood, properties);
            var path = Path.of("src/main/resources/trees/dynamic_trees_dimension_compat/leaves_properties", properties + ".json");
            var json = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
            assertEquals("undergarden:" + wood + "_leaves", json.get("primitive_leaves").getAsString());
            assertEquals("undergarden:" + wood + "_leaves", ImmersiveWeatheringLeafItems.itemForLeaves("undergarden:" + wood + "_leaves"));
        }
        for (String unrelated : new String[]{"minecraft:oak_leaves", "dynamictrees:oak_leaves", "other:grongle_leaves", "dynamic_trees_dimension_compat:unknown_leaves"}) {
            assertNull(ImmersiveWeatheringLeafItems.propertiesName(unrelated));
            assertEquals(unrelated, ImmersiveWeatheringLeafItems.itemForLeaves(unrelated));
        }
    }
}
