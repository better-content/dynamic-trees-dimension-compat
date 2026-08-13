package com.bettercontent.dynamictreesdimensioncompat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

final class BcDimTreesResourceTest {
    private static final Path TREE_ROOT = Path.of("src/main/resources/trees/dynamic_trees_dimension_compat");
    private static final Set<String> EXPECTED_SPECIES = Set.of(
            "dynamic_trees_dimension_compat:finley_wood",
            "dynamic_trees_dimension_compat:grongle",
            "dynamic_trees_dimension_compat:living_wood",
            "dynamic_trees_dimension_compat:silent_tree",
            "dynamic_trees_dimension_compat:smogstem",
            "dynamic_trees_dimension_compat:wigglewood"
    );

    @Test
    void speciesReferencePackagedFamiliesAndLeavesProperties() throws IOException {
        Set<String> families = resourceIds(TREE_ROOT.resolve("families"));
        Set<String> leavesProperties = resourceIds(TREE_ROOT.resolve("leaves_properties"));
        Set<String> packagedSpecies = resourceIds(TREE_ROOT.resolve("species"));

        assertEquals(EXPECTED_SPECIES, packagedSpecies);

        try (var paths = Files.list(TREE_ROOT.resolve("species"))) {
            for (Path path : paths.filter(BcDimTreesResourceTest::isJson).toList()) {
                JsonObject species = readObject(path);
                assertTrue(families.contains(species.get("family").getAsString()), "unknown family in " + path);
                assertTrue(leavesProperties.contains(species.get("leaves_properties").getAsString()),
                        "unknown leaves_properties in " + path);
                assertTrue(species.get("signal_energy").getAsDouble() > 0.0, "signal_energy must be positive in " + path);
                assertTrue(species.get("growth_rate").getAsDouble() > 0.0, "growth_rate must be positive in " + path);
                assertTrue(species.get("up_probability").getAsInt() > 0, "up_probability must be positive in " + path);
            }
        }
    }

    @Test
    void worldGenTargetsPackagedSpecies() throws IOException {
        JsonElement defaultWorldGen = JsonParser.parseReader(
                Files.newBufferedReader(TREE_ROOT.resolve("world_gen/default.json"))
        );
        var entries = defaultWorldGen.getAsJsonArray();
        assertTrue(entries.size() > 0, "expected dimension forest worldgen entries");
        entries.forEach(element -> {
            JsonObject apply = element.getAsJsonObject().getAsJsonObject("apply");
            Set<String> referenced = referencedSpecies(apply.get("species"));
            assertFalse(referenced.isEmpty(), "worldgen entry must reference species in " + element);
            referenced.stream()
                    .filter(id -> id.startsWith("dynamic_trees_dimension_compat:"))
                    .forEach(id -> assertTrue(EXPECTED_SPECIES.contains(id), "unknown species " + id));
            assertTrue(apply.has("density"), "dimension forest worldgen should set density in " + element);
            assertTrue(apply.has("chance"), "dimension forest worldgen should set chance in " + element);
        });
    }

    private static Set<String> referencedSpecies(JsonElement species) {
        if (species.isJsonPrimitive()) {
            return Set.of(species.getAsString());
        }
        JsonObject object = species.getAsJsonObject();
        if (object.has("random")) {
            return object.getAsJsonObject("random").keySet();
        }
        return Set.of();
    }

    private static Set<String> resourceIds(Path directory) throws IOException {
        try (var paths = Files.list(directory)) {
            Set<String> ids = paths.filter(BcDimTreesResourceTest::isJson)
                    .map(path -> "dynamic_trees_dimension_compat:" + path.getFileName().toString().replaceFirst("\\.json$", ""))
                    .collect(Collectors.toUnmodifiableSet());
            assertFalse(ids.isEmpty(), "expected resources in " + directory);
            return ids;
        }
    }

    private static boolean isJson(Path path) {
        return path.getFileName().toString().endsWith(".json");
    }

    private static JsonObject readObject(Path path) throws IOException {
        return JsonParser.parseReader(Files.newBufferedReader(path)).getAsJsonObject();
    }
}
