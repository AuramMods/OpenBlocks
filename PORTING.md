# PORTING.md

## Read Order Before Work
1. `PORTING_MEMORY.md`
2. `PORTING.md`
3. `PORTING_MANIFEST.md`

## Porting Strategy
- Preserve the registry scaffold and finish behavior by feature. Verify normal acquisition, interaction, persistence, synchronization, and presentation where applicable.
- Use quiet, headless server runs by default. Client verification needs a muted graphical session that leaves the user's foreground app in focus.

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
- [-] Reconcile shaped and shapeless recipes for all craftable features. All seven legacy custom recipes now have implementations.
- [x] Tags needed for gameplay parity and compatibility.
- [x] Cross-check that each registered block/item ID has matching model/lang assets.

Exit criteria:
- [ ] Data generation/resources run without missing-model or missing-lang spam for core objects.

Validation log:
- [x] Repaired the canvas recipe to use oak fence and narrowed legacy tags to the intended materials and dye colors. The standalone game server loaded the canvas recipe and verified ingredient matching on 2026-11-18.
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
- [x] Fixed client-side fluid texture crash path for `open_blocks:xpjuice` by wiring explicit `IClientFluidTypeExtensions` still/flowing texture keys in `OBFluidTypes` (prevents null texture lookups in third-party tank item renderers such as Mekanism creative fluid tanks), validated with `./gradlew compileJava runData` on 2026-02-09.
- [-] Started block-functionality depth sweep for legacy non-GUI blocks: `heal` now uses a dedicated ticking block entity aura (`OBHealBlock` + `OBHealBlockEntity`) that applies regeneration/saturation to nearby non-creative players every second, `path` now enforces support-below survival and self-drops when support is removed (`OBPathBlock`), and `sponge` now performs periodic radius liquid cleanup with lava burn fallback and border-fluid wake-up on removal (`OBSpongeBlock`), validated with `./gradlew compileJava runData` on 2026-02-09.
- [-] Continued block-functionality depth sweep for ladder domain: `ladder` now enforces north-face support survival (`OBLadderBlock`), and `rope_ladder` now supports chain survival (support block or ladder above), auto-extends downward on placement while consuming held stack count in survival, and cascades removal down the chain (`OBRopeLadderBlock`), validated with `./gradlew compileJava runData` on 2026-02-09.
- [-] Continued block-functionality depth sweep for elevator domain: `elevator` now has baseline teleport flow (`OBElevatorBlock`) using right-click for upward travel and sneak-right-click for downward travel with travel/blocker limits and safe destination checks, while `elevator_rotating` now extends this flow (`OBElevatorRotatingBlock`) with facing-based arrival yaw alignment; both are wired in `OBBlocks`, validated with `./gradlew compileJava runData` on 2026-02-09.

## Phase 3: Systems Skeleton (Breadth Gameplay Pass)
Goal: recreate cross-cutting systems in thin form before deep feature parity.

Checklist:
- [ ] Replace old GUI/container flow with modern menu/screen flow.
- [-] Rebuild capability equivalents (player/entity state).
- [-] Rebuild networking layer for events/RPC replacements. The elevator request is the first registered serverbound packet.
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
- [x] Transportation/domain: Hang Glider, Luggage, wearable crane, and Miracle Magnet turtle upgrade
- [-] Storage/domain: tanks, xp systems, vacuum hopper, donation station
- [-] Utility/domain: block breaker/placer/dropper, auto anvil/enchantment table
- [x] Visual/domain: canvas, paint, stencils, and glyphs
- [-] Entity/domain: mini-me, golden eye, and the remaining entities in the manifest
- [x] Fun/domain: mob trophies, Flim-Flam combat and luck, and the 17 prank actions.

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
- [x] Reproduce and repair the baseline defects in mixed elevator rotation, sponge handling of logged water, and grave placement fallback. Verified with the standalone GameTest server on 2026-11-18.
- [x] Restore distinct legacy crafting components across recipes, creative entries, and models.
- [x] Restore elevator colors and movement activation. Headless tests cover server behavior and item color through crafting, placement, and drops. A packaged client verified Space, Shift, color matching, and rotation in single-player using a silent virtual display.
- [x] Restore Jaded Ladder trapdoor behavior, directional rope ladders, and temporary scaffolding. The standalone suite verifies climbing, redstone, placement rollback, rope drop accounting, decay and fuel.
- [-] Restore state variants across recipes, creative entries, and models. Paint brushes, paint cans, canvas surfaces, prepared stencils, and glyphs are restored.
- [ ] Run a muted client in an isolated or background graphical session to verify models, transparency, and face culling.
- [ ] Reconcile crafting and acquisition against the full legacy recipe inventory.
- [x] Restore and verify the Flim-Flam snowball, teleport, mount, encase, dummy creeper, random potion, and skyblock actions.
- [x] Restore grave-owned loot, optional recovery files, the two-point shovel cost, and the persistent empty memorial. Verify rejected drops, older dump-backed graves, and backup recovery beyond ordinary player slots.
- [ ] Deepen `/ob_inventory` parity beyond current built-in + event-bridge baseline:
  - add first concrete subsystem consumers/producers for `OBInventoryEvent` payloads (legacy-style arbitrary subsystem parity in practice, not just schema support)
  - verify full inventory and external subsystem restore behavior against the legacy commands
- [x] Restore the grave placement event, legacy search order, height bounds, dirt base, destructive fallback, and optional consumption of a carried grave.
- [x] Restore grave facing, epitaph and model variants, blast resistance, and ambient skeleton and bat spawning.
- [ ] Finish legacy recipe migration for skipped recipe files and remaining metadata-collapsed color or subtype conversions.
- [x] Restore configurable sponge cleanup, Sponge on a Stick, scattered-stone path rendering, and the healing block's one-tick pulses.
- [x] Restore tank block and item storage, connected transfer and rendering, XP and bucket exchange, and the Liquid XP block assets.
- [x] Restore the XP Drain and redstone-powered XP Shower. The drain transfers whole XP into a fluid receiver, and the shower releases falling XP orbs that a drain can collect.
- [x] Restore the XP Bottler recipe, timed bottle production, directional item and fluid access, and configurable automation.
- [x] Restore the Vacuum Hopper's item and XP collection, separate output faces, pause control, and persistent storage.
- [x] Restore the Block Breaker, Block Placer, and Item Dropper, including redstone behavior, six-directional models, inventory access, and the Dropper's launch controls.
- [x] Restore the Auto Anvil and Auto Enchantment Table, including sided automation, Liquid XP costs, menus, bookshelf power, and the animated book.
- [x] Restore the Sprinkler's water and bone-meal cycles, growth, and animated spray, and the Donation Station's sample inventory, author links, and optional peripheral.
- [x] Restore the redstone Cannon, Pointer targeting, animated model and flight preview, item projectiles, and falling-projectile collection by the Vacuum Hopper.
- [x] Restore the XP Bucket's world-source pickup and creative visibility settings. The Forge bucket still supports fluid capabilities and tank exchange.
- [x] Restore the stone and wooden Big Buttons, including six-face placement, configurable redstone pulses, and wooden arrow activation.
- [x] Restore the folding Target's redstone deployment, accuracy-based output, and arrow recovery.
- [ ] Test and port the optional Flan's Mod projectile integration against its 1.20.1 implementation.
- [x] Restore the Fan's adjustable heading, analog airflow, config, and spinning blades, and the Bear Trap's jaws, mob capture, release and comparator output.
- [x] Restore Flag mounts, turning, all carpet colors, and color-preserving drops. Restore both Sky Block polarities, the torch conversion, and live sky rendering.
- [x] Restore the /dev/null inventory, world pickup filtering, overflow, forwarding block placement, and the true nesting advancement.
- [x] Restore Tasty Clay digestion, the Be Silly key, brick drops, the Bricks dropped statistic, and the Zoinks! advancement.
- [x] Restore Pedometer sampling, offhand use, route checks, original readout units, and its movement-driven item animation.
- [x] Restore Luggage deployment, owner following, item collection, packing, both inventory sizes, lightning upgrades, and the original animated models.
- [x] Restore Hang Glider deployment, steering, dive, fall protection, thermal lift, camera-specific rendering, and variometer controls.
- [x] Restore the wearable Crane Backpack, Crane Control, and player magnet with block and entity pickup, protected cargo placement, and synchronized models.
- [x] Restore the optional Miracle Magnet for CC: Tweaked turtles, including native crafting and equipping, Lua control, fuel, and left and right upgrade models.
- [x] Restore the Cartographer, world-saved terrain and liquid surveys, map resizing and cloning, and the assistant's progress display.
- [x] Restore the Projector's single-map inventory, rotating hologram, interactive preview, live survey updates, and optional CC: Tweaked map editing.
- [x] Restore the Paint Mixer, paint cans, dyed brushes and their recipes, face-by-face canvas painting, and the squeegee.
- [x] Restore the Drawing Table and prepared stencils, including multiple painted layers and sheet recovery.
- [x] Restore glyph selection, text printing, hand and wall models, and wall placement on the 1/16-block grid.
- [x] Restore Guide and Builder Guide shapes, controls, previews, survival building, creative filling, and the optional CC: Tweaked peripheral.
- [x] Restore imaginary blocks, Magic Pencil and Crayon placement, color recipes, and the four imagination glasses, including Technicolor chest loot.
- [x] Restore the Golden Egg's hatching sequence, optional block lift, and explosion, and Mini-Me's skin, foraging, and player pickup behavior.
- [x] Restore the Golden Eye's structure lock, rechargeable durability, and recoverable horizontal and downward flights.
- [x] Restore all 44 mob trophies, Looting-aware drops, miniature models, sounds, special effects, and cooldown persistence.
- [x] Restore the Village Highlighter's powered overlay, facing and active model, and redstone output for vacant beds.
- [x] Restore the Sleeping Bag's temporary sleep, armor swap, item recovery, and original fabric model.
- [x] Restore the Slimalyzer's slime-chunk detection, model indicator, and entry ping for carried and dropped stacks.
- [x] Restore the Sonic Glasses' wearable model, configurable dimming, and fading sound-source icons.
- [x] Restore the Wrench's face-based rotation and the Epic Eraser's lore removal and 15 uses.
- [x] Restore the Information Book with its catalogues and live recipe previews, and the Cursor with linked block interactions and XP costs.
- [x] Restore Unstable's armor blasts and landing bounce, Last Stand's configurable XP protection, and both enchantments' acquisition and book entries.
- [x] Restore Flim-Flam sword and armor acquisition, combat luck and cooldown, weighted selection, safety and list filters, item history, and the operator commands.
