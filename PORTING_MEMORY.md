# PORTING_MEMORY.md

> STEP 0 (REQUIRED): READ THIS FILE FIRST BEFORE DOING ANY PORT WORK.
> STEP 1: Open `PORTING.md` for current phase/checklist.
> STEP 2: Open `PORTING_MANIFEST.md` for exact IDs and source locations.

## Current Snapshot
- Legacy codebase analyzed: `old-1.12.2`.
- Analysis goal completed: breadth-first registry and system inventory.
- Core inventory files created at repo root:
  - `PORTING.md`
  - `PORTING_MEMORY.md`
  - `PORTING_MANIFEST.md`

## 1.20.1 Progress Snapshot
- Block/item registry baseline implemented in current project:
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/java/art/arcane/openblocks/OpenBlocks.java`
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/java/art/arcane/openblocks/registry/OBBlocks.java`
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/java/art/arcane/openblocks/registry/OBItems.java`
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/java/art/arcane/openblocks/registry/OBCreativeTabs.java`
- Registered in 1.20.1 scaffolding:
  - 41 block IDs (legacy names preserved)
  - 41 block items (one per block)
  - 30 standalone item IDs (legacy names preserved)
  - 1 creative tab listing all registered items in a single place
- Asset scaffolding generated and wired:
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/resources/assets/open_blocks/blockstates` (41)
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/resources/assets/open_blocks/models/block` (41)
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/resources/assets/open_blocks/models/item` (71)
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/resources/assets/open_blocks/lang/en_us.json`
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/resources/assets/open_blocks/textures/blocks` (legacy texture import)
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/resources/assets/open_blocks/textures/items` (legacy texture import)
- All currently registered IDs now have matching blockstate/model/lang coverage:
  - 41/41 blockstates
  - 41/41 block models
  - 71/71 item models
  - 41 block lang keys + 30 standalone item lang keys
- Validation:
  - `./gradlew compileJava` succeeds after registry + asset changes.
  - `./gradlew compileJava runData` succeeds (2026-02-08).
- Datagen runtime safety fix:
  - `build.gradle` now skips optional runtime jars from `extra-mods-1.20.1` when requested tasks include `runData`/`datagen`.
  - This prevents unrelated dev-runtime mods (AE2 etc.) from crashing datagen runs.
- Important namespace note:
  - Current mod namespace is `open_blocks` (project default), not `openblocks` yet.

## Critical Architecture Facts
- Main entrypoint is `old-1.12.2/src/main/java/openblocks/OpenBlocks.java`.
- Most object registration is centralized in `OpenBlocks` nested classes:
  - `OpenBlocks.Blocks` with `@RegisterBlock` annotations.
  - `OpenBlocks.Items` with `@RegisterItem` annotations.
  - `OpenBlocks.Fluids` static fluid instance(s).
  - Event-bus registry hooks for sounds, sync types, RPC methods, and network events.
- OpenBlocks depends heavily on OpenMods/OpenModsLib patterns.
- `OpenModsLib` submodule is not present in this snapshot checkout (`old-1.12.2/OpenModsLib` is empty).
- Features are often conditionally active: many subsystems check for `OpenBlocks.Blocks.* != null` or `OpenBlocks.Items.* != null` before registering behavior.

## High-Risk Porting Facts (Do Not Forget)
- OpenMods dependency footprint is very large:
  - `250` Java files import `openmods.*`.
  - `972` total `import openmods.*` lines.
- Old custom OpenMods registries exist and need a 1.20.1 strategy:
  - `SyncableObjectType`
  - `MethodEntry` (RPC)
  - `NetworkEventEntry`
- Old GUI/container flow uses `IGuiHandler` and `IHasGui`; this needs a modern menu/screen flow.
- Old capability attach patterns are widespread and include custom player/entity state:
  - `openblocks:bowels`
  - `openblocks:pedometer_state`
  - `openblocks:luck`
- Legacy `EntityRegistry.registerModEntity` and `DataFixer` calls are used.
- Legacy ore dictionary is used (`craftingTableWood`, `chestWood`).
- Old villager profession registration exists (`openblocks:radio`).
- Legacy creative tab behavior is NOT one-item-per-ID:
  - Many items expose multiple creative variants via `getSubItems` (paint can colors, elevator colors, glyph variants, stencil patterns, etc.).
  - Current 1.20.1 breadth scaffold intentionally shows one stack per registered ID only (parity pass later).

## Registry Scope to Preserve
- Blocks: 41
- Direct items: 30
- Fluids: 1 primary (`xpjuice`) + configurable accepted XP fluids
- Enchantments: 3 (`explosive`, `last_stand`, `flim_flam`)
- Sound events: 19 registered in code
- Mod entities: 11
- Block entities (tile entities): 35 tied to registered blocks (36 classes in package including base helpers)
- Custom recipes (code-registered): 7
- Commands: 3 (`flimflam`, `luck`, `ob_inventory`)
- Advancements/triggers: 2 custom triggers

## Known Oddities Worth Rechecking During Port
- `xp_bucket` blockstate references fluid string `liquidxp`, while code fluid ID is `xpjuice`.
- `blockstates/temp.json` exists and appears unused.
- Integration module registration lines in `OpenBlocks.preInit` are commented out:
  - `ModuleAdapters`
  - `ModuleTurtles`

## Always-Open File Order for New Sessions
1. `PORTING_MEMORY.md`
2. `PORTING.md`
3. `PORTING_MANIFEST.md`
4. `old-1.12.2/src/main/java/openblocks/OpenBlocks.java`
5. `old-1.12.2/src/main/java/openblocks/Config.java`

## Next Breadth Step (Planned)
- Continue Phase 1 breadth: add fluids/block entities/entities/sound/enchantment/menu/recipe serializer registries with placeholder implementations.
