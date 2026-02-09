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
- [-] Added a mechanical legacy-recipe conversion baseline: 185 shaped/shapeless legacy recipes from `old-1.12.2/assets/openblocks/recipes` now ported to `src/main/resources/data/open_blocks/recipes/legacy` (metadata collapsed to 1.20-safe item IDs and ore-dict ingredients translated to `open_blocks:legacy_ore_dict/*` item tags backed by baseline vanilla values); `openmods:enchanting` recipes are now handled by the dedicated `flim_flam_book` custom serializer path, validated with `./gradlew compileJava runData` on 2026-02-08.
- [x] Replaced legacy `openmods:enchanting` flim-flam recipe family with a 1.20 custom crafting recipe serializer (`open_blocks:flim_flam_book`) that recreates level scaling from emerald count (1-4), validated with `./gradlew compileJava runData` on 2026-02-08.
- [-] Added systems skeleton for legacy command IDs (`flimflam`, `luck`, `ob_inventory`) and custom advancement triggers (`open_blocks:brick_dropped`, `open_blocks:dev_null_stacked`) with 1.20 wiring; `luck` is capability-backed, `ob_inventory` has a file-backed inventory dump pipeline (main + built-in sub targets + death dumps), and `flimflam` now executes a breadth-stage effect registry for all 17 legacy IDs, validated with `./gradlew compileJava runData` on 2026-02-09.
- [-] Added capability skeleton for legacy player capability IDs (`open_blocks:luck`, `open_blocks:pedometer_state`, `open_blocks:bowels`) with registration + attach + clone-copy persistence, and switched `/luck` command state to capability-backed storage, validated with `./gradlew compileJava runData` on 2026-02-09.
- [x] Expanded legacy ore-dict compatibility tags in `src/main/resources/data/open_blocks/tags/items/legacy_ore_dict` from single-item placeholders to broader `forge`/`minecraft` tag membership across 34 files, validated with `./gradlew compileJava runData` on 2026-02-09.
- [-] Added initial gameplay event hooks for custom triggers/capability bridge in `OBAdvancementHooks` (`tasty_clay` consume -> bowels +1, brick toss -> `brick_dropped` with bowels decrement, periodic dev-null depth approximation -> `dev_null_stacked`), and updated item properties for parity (`tasty_clay` edible, `dev_null`/`generic_unstackable` unstackable), validated with `./gradlew compileJava runData` on 2026-02-09.
- [-] Expanded bowels parity hooks with death-drop behavior in `OBAdvancementHooks` (`LivingDropsEvent`: drop up to 16 bricks from stored bowels count, then clear), and updated clone behavior in `OBCapabilities` to not copy bowels on death clones, validated with `./gradlew compileJava runData` on 2026-02-09.
- [-] Added initial pedometer movement sampling hook: `OBAdvancementHooks` now ticks `OBCapabilities.PedometerState` on server player ticks when a pedometer exists in hotbar while tracking is active, and `OBItems.PEDOMETER` is now unstackable for baseline parity, validated with `./gradlew compileJava runData` on 2026-02-09.
- [-] Added explicit pedometer item-flow scaffold via new `OBPedometerItem` (right-click starts tracking, sneak-right-click resets, right-click while running prints report lines), expanded `OBCapabilities.PedometerState` report math helpers, switched pedometer sampling to run only while state is running, and added legacy pedometer message keys to `en_us.json`, validated with `./gradlew compileJava runData` on 2026-02-09.
- [-] Replaced `/ob_inventory` placeholder responses with a working breadth-stage dump backend (`OBInventoryStore`): `store` writes compressed `inventory-*.dat` files under world `data/`, `restore` loads main inventory data back to players, and `spawn` drops stored main inventory stacks (with optional slot), validated with `./gradlew compileJava runData` on 2026-02-09.
- [-] Replaced `/flimflam` placeholder responses with executable breadth-stage effect actions via new `OBFlimFlamEffects` (all legacy command effect IDs wired to thin server-side behaviors: inventory shuffle/disarm, potion/sound/entity prank effects, encase/skyblock, etc.), validated with `./gradlew compileJava runData` on 2026-02-09.
- [-] Expanded `/ob_inventory` breadth parity: dumps now include sub-inventory payloads (`armor`, `offhand`, `ender_chest`) with target-aware spawn/restore handling, and added automatic death-time inventory dump storage via `OBInventoryHooks` to command-consumable `inventory-*.dat` files, validated with `./gradlew compileJava runData` on 2026-02-09.
- [-] Added legacy-style inventory subsystem event bridge (`OBInventoryEvent`): `/ob_inventory store` now captures arbitrary event-provided sub-inventory payloads into dump `SubInventories`, `/ob_inventory restore` now publishes the decoded payload map back to listeners, and command target suggestions/spawn support now include serialized dynamic subsystem keys, validated with `./gradlew compileJava runData` on 2026-02-09.
- [-] Added grave gamerule + backup dump breadth skeleton: registered legacy-style gamerule key `openblocks:spawn_graves` (`OBGameRules`), added `OBGraveHooks` drop-time backup dumps (`type=grave`) gated by `keepInventory` + gamerule state, and extended `OBInventoryStore` with drop-list dump writes, validated with `./gradlew compileJava runData` on 2026-02-09.
- [-] Deepened grave parity from backup-only to place+claim baseline: added dedicated grave block/entity wiring (`OBGraveBlock`, `OBGraveBlockEntity`), upgraded `OBGraveHooks` to search/place graves and bind dump IDs on death, and extended `OBInventoryStore` with grave-claim helpers (`readDroppedItems`, `deleteDump`); successful grave placement now clears world drops and grave interaction now restores stored loot + removes the grave, validated with `./gradlew compileJava runData` on 2026-02-09.
- [-] Deepened grave parity with action-filter + interaction pass: added legacy-style `OBGraveDropsEvent` (`STORE`/`DROP`/`DELETE`) and wired `OBGraveHooks` to post/filter drop actions before storage/placement, plus grave interaction now shows stored death message on normal use and requires shovel-action interaction to claim loot from placed graves, validated with `./gradlew compileJava runData` on 2026-02-09.

## Phase 3: Systems Skeleton (Breadth Gameplay Pass)
Goal: recreate cross-cutting systems in thin form before deep feature parity.

Checklist:
- [ ] Replace old GUI/container flow with modern menu/screen flow.
- [-] Rebuild capability equivalents (player/entity state).
- [ ] Rebuild networking layer for events/RPC replacements.
- [-] Rebuild advancement trigger plumbing.
- [-] Rebuild command registration.
- [ ] Rebuild loot injection strategy.
- [ ] Rebuild villager/trade registration strategy.
- [-] Rebuild game rule registration strategy.
- [-] Rebuild world data persistence equivalents (map data, grave data dependencies, etc).

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
- [ ] Deepen `/flimflam` parity beyond current breadth-stage action wiring:
  - reconnect legacy luck/cost/weight and safe/blacklist gating behavior
  - tune per-effect details from thin stubs toward legacy-accurate behavior
- [ ] Deepen `/ob_inventory` parity beyond current built-in + event-bridge baseline:
  - add first concrete subsystem consumers/producers for `OBInventoryEvent` payloads (legacy-style arbitrary subsystem parity in practice, not just schema support)
  - reconnect gravestone/inventory-backend integrations to reuse the dump pipeline (grave placement + claim now wired; subsystem-specific restore UX and backend consumers still pending)
- [ ] Deepen grave parity from current place+claim skeleton:
  - expand `OBGraveDropsEvent` parity beyond current action filtering (hook concrete listeners/producers and mirror more legacy filtering/consumption flows)
  - carry richer grave metadata/behavior (owner restrictions, richer death-message formatting, base-placement/facing details) into block/entity state
  - harden grave placement edge-cases (destructive fallback policy, cross-dimension guarantees, failure-path UX/messages)
- [ ] Replace capability placeholders with gameplay hooks:
  - `pedometer_state`: expand from current explicit start/reset/report baseline to deeper parity (client-side speed property behavior + legacy unit/readout polish)
  - `bowels`: expand from current tasty-clay + brick-toss + death-drop hooks to full legacy behavior parity (keybound brick-drop path + sound/stat nuances)
  - `luck`: reconnect full flim-flam cooldown and forced-trigger logic (currently just raw value storage)
- [ ] Wire custom advancement triggers into gameplay events:
  - replace temporary hooks with legacy-accurate trigger sources (`brick_dropped` from boo/brick action path, `dev_null_stacked` from nested dev-null inventory depth)
- [ ] Finish legacy recipe migration for skipped recipe files:
  - revisit metadata-collapsed conversions to restore accurate color/subtype behavior where needed (generic/meta items, paintbrush variants, elevator/flag color outputs, etc.)
