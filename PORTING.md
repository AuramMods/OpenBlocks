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
- [ ] Create/verify central mod bootstrap wiring in current 1.20.1 project.
- [ ] Create deferred registers (or equivalent) for:
  - [ ] Blocks
  - [ ] Items
  - [ ] Fluids + fluid type + bucket
  - [ ] BlockEntityType
  - [ ] EntityType
  - [ ] SoundEvent
  - [ ] Enchantment
  - [ ] MenuType
  - [ ] RecipeSerializer (custom recipes)
- [ ] Recreate all legacy block IDs (from manifest) as stubs/placeholders where needed.
- [ ] Recreate all legacy item IDs (including meta-item strategy replacement plan).
- [ ] Recreate all legacy entity IDs as registered entity types.
- [ ] Recreate all legacy sound IDs in code/data.
- [ ] Establish compatibility mapping plan for old IDs/legacy names.

Exit criteria:
- [ ] Project compiles with all primary registries present.
- [ ] No missing references for canonical IDs in code.

## Phase 2: Data Asset Skeleton (Breadth Data Pass)
Goal: every registered object has minimum data assets so the game loads cleanly.

Checklist:
- [ ] Blockstates for all registered blocks.
- [ ] Item models for all registered items.
- [ ] Language entries for all objects.
- [ ] Sound definitions (`sounds.json`) aligned with code registrations.
- [ ] Basic loot tables where required.
- [ ] Basic recipes (or temporary placeholders) for each craftable feature.
- [ ] Tags needed for gameplay parity and compatibility.

Exit criteria:
- [ ] Data generation/resources run without missing-model or missing-lang spam for core objects.

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
- [ ] Start Phase 1 by creating or validating 1.20.1 registry scaffolding and listing each legacy ID into the new registry classes with placeholder implementations.
