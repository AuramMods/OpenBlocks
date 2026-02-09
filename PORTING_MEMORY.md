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
- Registry skeleton implemented in current project:
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/java/art/arcane/openblocks/OpenBlocks.java`
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/java/art/arcane/openblocks/registry/OBBlocks.java`
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/java/art/arcane/openblocks/registry/OBItems.java`
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/java/art/arcane/openblocks/registry/OBCreativeTabs.java`
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/java/art/arcane/openblocks/registry/OBFluidTypes.java`
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/java/art/arcane/openblocks/registry/OBFluids.java`
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/java/art/arcane/openblocks/registry/OBBlockEntities.java`
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/java/art/arcane/openblocks/registry/OBEntities.java`
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/java/art/arcane/openblocks/registry/OBSounds.java`
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/java/art/arcane/openblocks/registry/OBEnchantments.java`
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/java/art/arcane/openblocks/registry/OBMenuTypes.java`
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/java/art/arcane/openblocks/registry/OBRecipeSerializers.java`
- Registered in 1.20.1 scaffolding:
  - 41 block IDs (legacy names preserved)
  - 41 block items (one per block)
  - 30 standalone item IDs (legacy names preserved)
  - 1 creative tab listing all registered items in a single place
- Asset scaffolding generated and wired:
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/resources/assets/open_blocks/blockstates` (41)
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/resources/assets/open_blocks/models/block` (41)
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/resources/assets/open_blocks/models/item` (71)
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/resources/assets/open_blocks/sounds.json`
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/resources/assets/open_blocks/sounds` (legacy `.ogg` set copied)
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/resources/assets/open_blocks/lang/en_us.json`
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/resources/assets/open_blocks/textures/block` (legacy texture import, 1.20 atlas path)
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/resources/assets/open_blocks/textures/item` (legacy texture import, 1.20 atlas path)
- All currently registered IDs now have matching blockstate/model/lang coverage:
  - 41/41 blockstates
  - 41/41 block models
  - 71/71 item models
  - 41 block lang keys + 30 standalone item lang keys
- Validation:
  - `./gradlew compileJava` succeeds after registry + asset changes.
  - `./gradlew compileJava runData` succeeds (2026-02-08).
- Datagen baseline (server data) added:
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/java/art/arcane/openblocks/datagen/OBDataGenerators.java`
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/java/art/arcane/openblocks/datagen/OBLootTableProvider.java`
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/java/art/arcane/openblocks/datagen/OBBlockLootSubProvider.java`
  - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/main/java/art/arcane/openblocks/datagen/OBBlockTagsProvider.java`
  - Generated 41 self-drop block loot tables under:
    - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/generated/resources/data/open_blocks/loot_tables/blocks`
  - Generated baseline vanilla block-tag injections:
    - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/generated/resources/data/minecraft/tags/blocks/mineable/pickaxe.json`
    - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/generated/resources/data/minecraft/tags/blocks/needs_stone_tool.json`
    - `/Users/cyberpwn/development/workspace/AuramMods/OpenBlocks/src/generated/resources/data/minecraft/tags/blocks/climbable.json`
  - Revalidated with:
    - `./gradlew compileJava`
    - `./gradlew runData`
- Recipe placeholder baseline added:
  - `src/main/resources/data/open_blocks/recipes/crayon_merge.json`
  - `src/main/resources/data/open_blocks/recipes/crayon_mix.json`
  - `src/main/resources/data/open_blocks/recipes/crayon_glasses.json`
  - `src/main/resources/data/open_blocks/recipes/map_clone.json`
  - `src/main/resources/data/open_blocks/recipes/map_resize.json`
  - `src/main/resources/data/open_blocks/recipes/golden_eye_recharge.json`
  - `src/main/resources/data/open_blocks/recipes/epic_eraser_action.json`
  - These are temporary type-only placeholders for custom serializers; recipe logic parity remains a later depth pass.
- Legacy missing-mapping compatibility baseline added:
  - `src/main/java/art/arcane/openblocks/registry/OBMissingMappings.java`
  - Subscribed on Forge event bus (`MissingMappingsEvent` is not a MOD-bus event).
  - Handles namespace migration:
    - `openblocks:* -> open_blocks:*`
    - `OpenBlocks:* -> open_blocks:*`
  - Handles key alias migration for known legacy IDs across blocks/items/fluids/block entities/menus:
    - examples: `vacuumhopper -> vacuum_hopper`, `blockPlacer -> block_placer`, `filledbucket -> xp_bucket`, `liquidxp -> xpjuice`
  - Alias lookup now includes both original and lowercased keys to survive historical casing normalization in saved data.
  - Validation:
    - `./gradlew compileJava runData` succeeds with remap hook present (2026-02-08).
- Datagen runtime safety fix:
  - `build.gradle` now skips optional runtime jars from `extra-mods-1.20.1` when requested tasks include `runData`/`datagen`.
  - This prevents unrelated dev-runtime mods (AE2 etc.) from crashing datagen runs.
- Client render-path fix:
  - 1.20.1 expects atlas texture roots under `textures/block` and `textures/item`.
  - Legacy-style plural paths (`textures/blocks`, `textures/items`) caused purple-black fallback with `Missing textures in model open_blocks:*` warnings.
  - Models now reference `open_blocks:block/...` and `open_blocks:item/...`.
- Legacy block model pass (shape-focused) completed:
  - Imported legacy block model files from `old-1.12.2/src/main/resources/assets/openblocks/models/block` into `src/main/resources/assets/open_blocks/models/block`.
  - Imported legacy model textures to `src/main/resources/assets/open_blocks/textures/model` (and `textures/misc`).
  - Converted model namespace/path refs to 1.20-style namespace/path (`open_blocks:*`, `block/item/model` texture roots).
  - Added static wrappers for blocks that used OpenMods dynamic blockstate loaders in 1.12:
    - `sprinkler -> sprinkler_static`
    - `target -> target_inactive`
    - `flag -> flag_ground`
    - `grave -> grave_ground`
    - `golden_egg -> egg`
    - `fan -> fan_frame`
    - `tank -> tank_frame`
    - `trophy -> trophy_base`
    - `vacuum_hopper -> vacuum_hopper_body`
    - `village_highlighter -> village`
    - `big_button`/`big_button_wood` -> `big_button_inactive` with vanilla stone/oak textures
  - Added a thin non-full `path` model.
  - Validation: no missing `open_blocks:*` refs across currently registered block/item models.
- Static render/collision parity pass (2026-02-08, follow-up):
  - Added client render-layer setup for transparency-sensitive blocks:
    - `src/main/java/art/arcane/openblocks/client/OBClientRenderLayers.java`
    - Uses `RenderType.cutout()` for ladder/rope ladder/path/xp drain/sprinkler/flag/scaffolding/paint can/guide/builder guide/canvas.
    - Uses `RenderType.translucent()` for `canvas_glass`.
  - Expanded non-occlusion + custom voxel shape mapping in:
    - `src/main/java/art/arcane/openblocks/registry/OBBlocks.java`
    - Added/updated shapes for `path`, `xp_drain`, `beartrap`, `cannon`, `sprinkler`, `target`, `paint_mixer`, `projector`, `xp_shower`, and others.
    - Added empty collision overrides for `path`, `cannon`, and `flag` to allow walking through non-solid visual space.
  - Extended shape block implementation:
    - `src/main/java/art/arcane/openblocks/block/OBShapeBlock.java`
    - Supports separate outline and collision shapes.
  - Fixed concrete missing-texture warning from `run/logs/latest.log`:
    - Warning was `Missing textures in model open_blocks:paint_mixer` for `open_blocks:model/paint_mixer`.
    - Model updated to `open_blocks:block/paint_mixer_model`.
    - Added atlas-safe texture file: `src/main/resources/assets/open_blocks/textures/block/paint_mixer_model.png`.
  - Ladder model switched from full cube wrapper to trapdoor-open template for non-full visual geometry:
    - `src/main/resources/assets/open_blocks/models/block/ladder.json`
  - Validation after these changes:
    - `./gradlew compileJava runData` passes.
- Important namespace note:
  - Current mod namespace is `open_blocks` (project default), not `openblocks` yet.
- Recipe corpus size note (legacy source):
  - `old-1.12.2/src/main/resources/assets/openblocks/recipes` has 189 recipe files:
    - 107 `forge:ore_shaped`
    - 78 `forge:ore_shapeless`
    - 4 `openmods:enchanting`
  - Main conversion blockers for a breadth-pass port are ore-dict/tag translation and metadata-based outputs (color/state variants).
- Recipe breadth-pass conversion now in place:
  - Added `src/main/resources/data/open_blocks/recipes/legacy` with 54 converted crafting recipes.
  - Conversion shape:
    - `forge:ore_shaped` -> `minecraft:crafting_shaped`
    - `forge:ore_shapeless` -> `minecraft:crafting_shapeless`
    - namespace remap `openblocks:*` -> `open_blocks:*`
  - Temporary compromise used for ore dictionary ingredients: mapped to concrete vanilla items (not final tag-based parity yet).
  - Skipped files currently:
    - 91 with non-zero `result.data`
    - 40 with non-zero ingredient `data`
    - 4 `openmods:enchanting`
  - Validation:
    - `./gradlew compileJava runData` succeeds after adding these 54 recipes.
    - `run-data/logs/latest.log` scan showed no recipe parse/load errors in datagen run.

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
- Continue Phase 2 breadth: expand recipe coverage beyond current custom-recipe placeholders.
- Draft legacy compatibility/remap mapping plan (`openblocks` namespace + legacy alias IDs -> `open_blocks` canonical IDs).
