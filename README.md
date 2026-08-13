# Better Content Dynamic Trees: Dimensions

Pack-local Dynamic Trees addon for dimension forests that do not have maintained 1.20.1 addons.

Currently covers:

- Blue Skies: bluebright, dusk, frostbright, maple, starlit.
- The Undergarden: grongle, smogstem, wigglewood.
- The Finley Dimension Remastered: finley wood, living wood.
- Call From The Depths: silent tree.

Run:

```sh
./gradlew test
./gradlew runData
./gradlew build
```

Runtime-only compatibility mods resolve from Maven coordinates in `build.gradle`. Do not commit downloaded mod jars under `libs/`.

## Community and support

For modpack and mod discussion, playtest feedback, and bug reports, join the [Better Content Discord](https://discord.gg/EkRnZbzqS9).

## Canonical identity

- Repository and Gradle project: `dynamic-trees-dimension-compat`
- Mod ID and resource namespace: `dynamic_trees_dimension_compat`
- Maven group: `com.bettercontent`
- Runtime artifact: `build/libs/dynamic-trees-dimension-compat-<version>.jar`

The canonical identity is a clean break. Legacy mod IDs, resource namespaces, configuration paths, commands, network channels, and saved-data keys are not migrated or aliased.
