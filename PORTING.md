# PORTING.md

## Read Order Before Work
1. `PORTING_MEMORY.md`
2. `PORTING.md`
3. `PORTING_MANIFEST.md`

## Porting Strategy
- Breadth-first first, depth second.
- Keep IDs stable first, then implement behavior.
- Avoid deep rewrites early; get full registry coverage across all systems before polishing internals.

## Status Legend
- `[ ]` not started
- `[-]` in progress / partial
- `[x]` complete

## Phase 0: Legacy Discovery and Inventory (Breadth Baseline)
Goal: build a reliable map of everything that must exist in 1.20.1.

Checklist:
- [x] Scan old code structure and main entrypoints.
- [x] Enumerate block and item registrations from code annotations.
- [x] Enumerate fluid, sound, enchantment, entity, and recipe registries.
- [x] Enumerate tile entities, containers, GUIs, and major event handlers.
- [x] Enumerate network/RPC/sync custom registries.
- [x] Enumerate capabilities, commands, advancements, loot injections, villager/game-rule hooks.
- [x] Create and populate `PORTING_MEMORY.md`.
- [x] Create and populate `PORTING_MANIFEST.md`.

Deliverables:
- `PORTING_MEMORY.md`
- `PORTING_MANIFEST.md`
- `PORTING.md` (this file)

## Phase 1: 1.20.1 Registry Skeleton (Breadth Port Pass)
Goal: all major registry objects exist in 1.20.1 with canonical IDs, even if behavior is minimal.

Checklist:
- [x] Create/verify central mod bootstrap wiring in current 1.20.1 project.
- [x] Create deferred registers (or equivalent) for:
  - [x] Blocks
  - [x] Items
  - [x] Creative mode tab
  - [x] Fluids + fluid type + bucket
  - [x] BlockEntityType
  - [x] EntityType
  - [x] SoundEvent
  - [x] Enchantment
  - [x] MenuType
  - [x] RecipeSerializer (custom recipes)
- [x] Recreate all legacy block IDs (from manifest) as stubs/placeholders where needed.
- [x] Recreate all legacy item IDs (including meta-item strategy replacement plan).
- [x] Recreate all legacy entity IDs as registered entity types.
- [x] Recreate all legacy sound IDs in code/data.
- [x] Establish compatibility mapping plan for old IDs/legacy names.
- [x] Generate asset scaffolding for blockstates/models/lang so registry visibility can be verified quickly.
- [x] Replace placeholder stone/iron assets with legacy texture-backed block/item models for all currently registered IDs.
- [x] Ensure datagen run path works for this project workflow (`./gradlew compileJava runData`).

Exit criteria:
- [x] Project compiles with all primary registries present.
- [x] No missing references for canonical IDs in code.

## Phase 2: Data Asset Skeleton (Breadth Data Pass)
Goal: every registered object has minimum data assets so the game loads cleanly.

Checklist:
- [x] Blockstates for all registered blocks.
- [x] Item models for all registered items.
- [x] Language entries for all objects.
- [x] Sound definitions (`sounds.json`) aligned with code registrations.
- [x] Basic loot tables where required.
- [-] Basic recipes (or temporary placeholders) for each craftable feature.
- [x] Tags needed for gameplay parity and compatibility.
- [x] Cross-check that each registered block/item ID has matching model/lang assets.

Exit criteria:
- [ ] Data generation/resources run without missing-model or missing-lang spam for core objects.

Validation log:
- [x] `./gradlew compileJava` succeeds after registry + asset changes.
- [x] `./gradlew compileJava runData` succeeds (as of 2026-02-08) after isolating datagen runs from optional `extra-mods-1.20.1` jars.
- [x] Added initial non-full-block render/collision parity pass for legacy-style models (cutout/translucent setup + custom voxel/collision shapes) and revalidated with `./gradlew compileJava runData` on 2026-02-08.
- [x] Added datagen wiring and generated 41 self-drop block loot tables under `src/generated/resources/data/open_blocks/loot_tables/blocks` (validated with `./gradlew compileJava` and `./gradlew runData` on 2026-02-08).
- [x] Added baseline block tags via datagen (`minecraft:mineable/pickaxe`, `minecraft:needs_stone_tool`, and `minecraft:climbable` for ladder/rope_ladder/scaffolding) and revalidated with `./gradlew compileJava` + `./gradlew runData` on 2026-02-08.
- [x] Added placeholder JSON definitions for all 7 legacy custom recipes under `src/main/resources/data/open_blocks/recipes` (types only; behavior parity still pending) and revalidated with `./gradlew compileJava runData` on 2026-02-08.
- [x] Added legacy ID compatibility remap hook in `OBMissingMappings` (namespace remap `openblocks`/`OpenBlocks` -> `open_blocks`, plus legacy alias paths including camelCase/lowercase variants) and revalidated with `./gradlew compileJava runData` on 2026-02-08.
- [-] Added a mechanical legacy-recipe conversion baseline: 185 shaped/shapeless legacy recipes from `old-1.12.2/assets/openblocks/recipes` now ported to `src/main/resources/data/open_blocks/recipes/legacy` (metadata collapsed to 1.20-safe item IDs and ore-dict ingredients translated to `open_blocks:legacy_ore_dict/*` item tags backed by baseline vanilla values); only 4 `openmods:enchanting` recipes remain unported, validated with `./gradlew compileJava runData` on 2026-02-08.
- [x] Replaced legacy `openmods:enchanting` flim-flam recipe family with a 1.20 custom crafting recipe serializer (`open_blocks:flim_flam_book`) that recreates level scaling from emerald count (1-4), validated with `./gradlew compileJava runData` on 2026-02-08.

## Phase 3: Systems Skeleton (Breadth Gameplay Pass)
Goal: recreate cross-cutting systems in thin form before deep feature parity.

Checklist:
- [ ] Replace old GUI/container flow with modern menu/screen flow.
- [ ] Rebuild capability equivalents (player/entity state).
- [ ] Rebuild networking layer for events/RPC replacements.
- [ ] Rebuild advancement trigger plumbing.
- [ ] Rebuild command registration.
- [ ] Rebuild loot injection strategy.
- [ ] Rebuild villager/trade registration strategy.
- [ ] Rebuild game rule registration strategy.
- [ ] Rebuild world data persistence equivalents (map data, grave data dependencies, etc).

Exit criteria:
- [ ] Server and client run with all major systems wired (even if behavior is simplified).

## Phase 4: Feature Depth Passes (By Domain)
Goal: move from "exists" to "works correctly".

Suggested order:
- [ ] Transportation/domain: elevators, rope ladder, hang glider, crane/magnet
- [ ] Storage/domain: tanks, xp systems, vacuum hopper, donation station
- [ ] Utility/domain: block breaker/placer/dropper, auto anvil/enchantment table
- [ ] Visual/domain: canvas/paint/stencil/glyph/projector/sky block
- [ ] Entity/domain: luggage/cartographer/mini-me/golden eye/etc
- [ ] Fun/domain: trophies, flim-flam, dev null, pedometer, sounds/effects

Per-feature checklist template:
- [ ] Registration complete
- [ ] Data assets complete
- [ ] Interaction logic complete
- [ ] Networking/state sync complete
- [ ] Save/load complete
- [ ] Basic test pass complete

## Phase 5: Compatibility and Hardening
Goal: stabilize and protect the port.

Checklist:
- [ ] Validate old-world migration behavior where practical.
- [ ] Revisit legacy ID mapping/remap behavior.
- [ ] Add regression tests for critical gameplay flows.
- [ ] Multiplayer sanity pass.
- [ ] Performance pass for heavy systems (render/network/tick).
- [ ] Cleanup dead stubs and document final architecture.

## Immediate Next Task
- [ ] Have user run a quick in-game visual sweep (no `runClient` on agent side) to confirm the latest transparency/collision changes removed purple-black model regressions and face-culling issues.
- [ ] Extend recipe coverage beyond custom special recipe placeholders (core craft paths for major blocks/items).
- [ ] Finish legacy recipe migration for skipped recipe files:
  - review and expand `open_blocks:legacy_ore_dict/*` tag membership so cross-mod ingredients are accepted where practical
  - revisit metadata-collapsed conversions to restore accurate color/subtype behavior where needed (generic/meta items, paintbrush variants, elevator/flag color outputs, etc.)
