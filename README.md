# OpenBlocks (Forge 1.12.2 -> Forge 1.20.1 Port)

## What This Project Is
This repository is a staged port of the original OpenBlocks mod from Forge 1.12.2 to Forge 1.20.1.

- Legacy source lives in `old-1.12.2`.
- New port source lives under `src/main/java/art/arcane/openblocks` and `src/main/resources/assets/open_blocks`.
- The mod ID in the current port is `open_blocks`.

## What We Are Doing
The port strategy is breadth-first, then depth-first:

1. Recreate the full registry surface and IDs first.
2. Ensure every registered object has basic data/models/lang.
3. Rebuild systems (menus, networking, capabilities, etc.) in thin form.
4. Deep-port behavior and polish feature-by-feature.

This avoids getting stuck perfecting one feature while the rest of the mod is still missing.

## How To Check Progress
Use these files in this exact order:

1. `PORTING_MEMORY.md`
2. `PORTING.md`
3. `PORTING_MANIFEST.md`

What each file is for:

- `PORTING_MEMORY.md`: session-critical notes and "do not forget" context.
- `PORTING.md`: phase plan + checklist status for active work.
- `PORTING_MANIFEST.md`: canonical inventory of legacy IDs, registries, and source locations.

## Progress Workflow (Per Session)
1. Read the three docs in order above.
2. Pick the next unchecked item in `PORTING.md`.
3. Implement the smallest breadth-safe slice.
4. Validate with:
   - `./gradlew compileJava`
   - `./gradlew runData`
5. Update `PORTING.md` and `PORTING_MEMORY.md`.
6. If new IDs/sources are discovered, update `PORTING_MANIFEST.md`.

## Command Loop
Primary validation loop for this port:

- `./gradlew compileJava`
- `./gradlew runData`
- optionally `./gradlew compileJava runData`

This project intentionally uses compile/datagen as the default verification path during scaffold stages.

## High-Level Scope Snapshot
From current manifest inventory:

- 41 legacy block IDs
- 30 legacy direct item IDs (+ block items)
- 1 primary fluid (`xpjuice`)
- 3 enchantments
- 19 sound events
- 11 entities
- 35 block entities (tile entity equivalents)
- 7 custom recipe registrations
- 3 legacy command IDs now wired in Brigadier form (`flimflam`, `luck`, `ob_inventory`)
- 3 legacy player capability IDs now scaffolded (`open_blocks:luck`, `open_blocks:pedometer_state`, `open_blocks:bowels`)
- 2 custom advancement triggers scaffolded (`open_blocks:brick_dropped`, `open_blocks:dev_null_stacked`)

Current breadth scaffolding highlights:
- Legacy missing-mapping compatibility is wired (`openblocks`/`OpenBlocks` -> `open_blocks` + legacy alias IDs).
- Model/texture/lang scaffolding is in place for all currently registered block/item IDs.
- A first recipe breadth-pass is in place: 185 legacy shaped/shapeless recipes are mechanically converted under `src/main/resources/data/open_blocks/recipes/legacy`, with legacy ore-dict names routed through `src/main/resources/data/open_blocks/tags/items/legacy_ore_dict`.
- Legacy ore-dict compatibility tags were expanded beyond single-item placeholders to broader `forge`/`minecraft` groups with fallback items for better cross-mod ingredient matching.
- Legacy flim-flam enchanting recipes are replaced by a custom 1.20 recipe serializer (`open_blocks:flim_flam_book`) that preserves level scaling from emerald cost.
- Legacy command and trigger IDs now exist in 1.20.1 scaffolding, so world/server content referencing those IDs has baseline continuity while deeper behavior is ported.
- Legacy player capabilities are now registered/attached and clone-persisted in 1.20.1 scaffolding, with `/luck` already switched to capability-backed state.
- Initial trigger-to-gameplay hooks are now present via Forge events (`tasty_clay` -> bowels, brick toss -> `brick_dropped`, placeholder dev-null depth scan -> `dev_null_stacked`), and key item properties were aligned (`tasty_clay` edible, `dev_null`/`generic_unstackable` unstackable).
- Datagen loop is stable with `./gradlew compileJava runData`.

## Directory Map
- `old-1.12.2`: original mod source/resources for reference
- `src/main/java/art/arcane/openblocks`: 1.20.1 Java port
- `src/main/resources/assets/open_blocks`: 1.20.1 assets
- `PORTING.md`: phase/checklist tracker
- `PORTING_MEMORY.md`: continuity memory
- `PORTING_MANIFEST.md`: registry/source manifest

## Project Goal
Deliver a maintainable Forge 1.20.1 OpenBlocks port with stable ID coverage first, then full gameplay parity and polish.
