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
