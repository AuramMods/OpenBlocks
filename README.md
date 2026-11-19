# OpenBlocks for Forge 1.20.1

This repository ports OpenBlocks from Forge 1.12.2 to Minecraft 1.20.1. The 1.20.1 mod ID is `open_blocks`. The development profile uses Java 17 and Forge 47.4.10.

## Build and test

```sh
./gradlew -PopenBlocksStandalone runData
./gradlew -PopenBlocksStandalone jar reobfJar
```

The distributable jar is written to `build/libs`. The Gradle `build` task also deploys to the configured Prism instance. Use the packaging tasks above for isolated builds.

Run the standalone gameplay suite in a disposable directory. Set `eula=true` for the test server after accepting the Minecraft EULA.

```sh
run_dir=$(mktemp -d)
mkdir -p "$run_dir/gametest"
printf 'eula=true\n' > "$run_dir/gametest/eula.txt"
./gradlew -PopenBlocksStandalone -PopenBlocksRunDir="$run_dir" runGameTestServer
```

The standalone profile excludes the optional development jars in `extra-mods-1.20.1`. For packaged multiplayer checks, use a disposable dedicated server with `--nogui`. For client checks, set `soundCategory_master:0.0` before launch, use an isolated display, and stop the test processes afterward.

## Code and data

- `old-1.12.2` contains the legacy source and assets used as the gameplay reference.
- `src/main/java/art/arcane/openblocks` contains the Forge port. `OpenBlocks` registers the mod content, configuration, network channel, and available integrations. Mechanics live with their blocks, items, entities, and gameplay event handlers.
- `src/main/resources` contains the hand-authored assets, recipes, tags, and other data. `src/generated/resources` contains datagen output.
- `src/gameTest` contains gameplay tests and fixtures. The release jar excludes these files.

The port preserves the established `open_blocks` IDs and supports the tested registry aliases from earlier 1.20.1 saves. The `/ob_inventory` commands can read OpenModsLib backups containing supported vanilla and OpenBlocks items. Native 1.12.2 worlds require a separate conversion before opening in 1.20.1.

For a new porting session, read [PORTING_MEMORY.md](PORTING_MEMORY.md), [PORTING.md](PORTING.md), and [PORTING_MANIFEST.md](PORTING_MANIFEST.md) in that order. They record runtime verification, the remaining work, and the legacy feature inventory.
