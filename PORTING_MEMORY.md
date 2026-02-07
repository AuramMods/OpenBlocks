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
- Build 1.20.1 registry skeleton first (all IDs present, minimal behavior), then deepen per-feature.
