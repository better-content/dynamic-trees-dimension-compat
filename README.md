# Better Content Dynamic Trees: Dimensions — validation-only

## Scope and authority

This repository is validation-only; The Undergarden is retired from the pack. Read
[local instructions](AGENTS.md) and the [shared policy index](../../better-content-modpack/docs/README.md).


Pack-local Dynamic Trees addon for The Undergarden's 1.20.1 forests.

It owns the grongle, smogstem, and wigglewood species, their generated resources and soil aliases,
and delayed chunk decoration for the four matching Undergarden forest biomes.

Run:

```sh
./gradlew verifyFast
./gradlew runData
./gradlew verifyFull
```

The Undergarden is a required runtime dependency and resolves from Maven in `build.gradle`.

## Community and support

For modpack and mod discussion, playtest feedback, and bug reports, join the [Better Content Discord](https://discord.gg/EkRnZbzqS9).

## Canonical identity

- Repository and Gradle project: `dynamic-trees-dimension-compat`
- Mod ID and resource namespace: `dynamic_trees_dimension_compat`
- Maven group: `com.bettercontent`
- Runtime artifact: `build/libs/dynamic-trees-dimension-compat-<version>.jar`

The canonical identity is a clean break. Legacy mod IDs, resource namespaces, configuration paths, commands, network channels, and saved-data keys are not migrated or aliased.
