# PORTING_MANIFEST.md

## Purpose
- Canonical breadth-first inventory of registries, IDs, and source locations in the 1.12.2 codebase.
- Use this file as the quick lookup map before touching implementation.
- Also tracks current 1.20.1 scaffold coverage and where each breadth-phase registry now lives.

## Source Scope
- Legacy root: `old-1.12.2`
- Main code root: `old-1.12.2/src/main/java/openblocks`
- Main assets root: `old-1.12.2/src/main/resources/assets/openblocks`

## Current 1.20.1 Port Snapshot (updated 2026-11-18)
Current source roots:
- Java root: `src/main/java/art/arcane/openblocks`
- Assets root: `src/main/resources/assets/open_blocks`

Current registry classes:
- Mod bootstrap: `src/main/java/art/arcane/openblocks/OpenBlocks.java`
- Blocks register: `src/main/java/art/arcane/openblocks/registry/OBBlocks.java`
- Items register: `src/main/java/art/arcane/openblocks/registry/OBItems.java`
- Creative tab register: `src/main/java/art/arcane/openblocks/registry/OBCreativeTabs.java`
- Fluid type register: `src/main/java/art/arcane/openblocks/registry/OBFluidTypes.java`
- Fluid/block register: `src/main/java/art/arcane/openblocks/registry/OBFluids.java`
- Block entity register: `src/main/java/art/arcane/openblocks/registry/OBBlockEntities.java`
- Entity register: `src/main/java/art/arcane/openblocks/registry/OBEntities.java`
- Sound register: `src/main/java/art/arcane/openblocks/registry/OBSounds.java`
- Custom statistic register for Bricks dropped: `src/main/java/art/arcane/openblocks/registry/OBStats.java`
- Enchantment register: `src/main/java/art/arcane/openblocks/registry/OBEnchantments.java`
- Menu register: `src/main/java/art/arcane/openblocks/registry/OBMenuTypes.java`
- Recipe serializer register: `src/main/java/art/arcane/openblocks/registry/OBRecipeSerializers.java`
- Custom pedometer item behavior:
  - `src/main/java/art/arcane/openblocks/item/OBPedometerItem.java`
  - `src/main/java/art/arcane/openblocks/player/OBPedometerTracking.java`
  - `src/main/java/art/arcane/openblocks/client/OBPedometerRendering.java`
- Command registration (Forge bus): `src/main/java/art/arcane/openblocks/command/OBCommands.java`
- Command inventory dump backend: `src/main/java/art/arcane/openblocks/command/OBInventoryStore.java`
- Command inventory death-dump hook: `src/main/java/art/arcane/openblocks/command/OBInventoryHooks.java`
- Command inventory subsystem event bridge: `src/main/java/art/arcane/openblocks/api/OBInventoryEvent.java`
- Grave drop action event API: `src/main/java/art/arcane/openblocks/api/OBGraveDropsEvent.java`
- Grave block + placement/claim wiring:
  - `src/main/java/art/arcane/openblocks/block/OBGraveBlock.java`
  - `src/main/java/art/arcane/openblocks/block/entity/OBGraveBlockEntity.java`
  - `src/main/java/art/arcane/openblocks/grave/OBGraveHooks.java`
- Block functionality sweep classes:
  - `src/main/java/art/arcane/openblocks/block/OBElevatorBlock.java`
  - `src/main/java/art/arcane/openblocks/block/OBElevatorRotatingBlock.java`
  - `src/main/java/art/arcane/openblocks/block/OBHealBlock.java`
  - `src/main/java/art/arcane/openblocks/block/entity/OBHealBlockEntity.java`
  - `src/main/java/art/arcane/openblocks/block/OBLadderBlock.java`
  - `src/main/java/art/arcane/openblocks/block/OBPathBlock.java`
  - `src/main/java/art/arcane/openblocks/block/OBRopeLadderBlock.java`
  - `src/main/java/art/arcane/openblocks/block/OBSpongeBlock.java`
- Game rule registration: `src/main/java/art/arcane/openblocks/world/OBGameRules.java`
- Command flimflam effect registry: `src/main/java/art/arcane/openblocks/command/OBFlimFlamEffects.java`
- Capability registration/attachment:
  - `src/main/java/art/arcane/openblocks/capability/OBCapabilities.java`
- Custom advancement trigger bootstrap:
  - `src/main/java/art/arcane/openblocks/advancement/OBCriterions.java`
  - `src/main/java/art/arcane/openblocks/advancement/OBBrickDroppedTrigger.java`
  - `src/main/java/art/arcane/openblocks/advancement/OBDevNullStackTrigger.java`
- Non-full shape helper block: `src/main/java/art/arcane/openblocks/block/OBShapeBlock.java`
- Client render-layer setup: `src/main/java/art/arcane/openblocks/client/OBClientRenderLayers.java`
- Datagen entrypoint: `src/main/java/art/arcane/openblocks/datagen/OBDataGenerators.java`
- Datagen loot providers:
  - `src/main/java/art/arcane/openblocks/datagen/OBLootTableProvider.java`
  - `src/main/java/art/arcane/openblocks/datagen/OBBlockLootSubProvider.java`
- Datagen tag provider:
  - `src/main/java/art/arcane/openblocks/datagen/OBBlockTagsProvider.java`

Current scaffold counts:
- Block IDs registered: 41
- Item IDs registered: 83 (41 block items + 42 standalone, including 2 compatibility IDs)
- Creative tabs registered: 1 (`open_blocks:main`)
- Fluid types registered: 1 (`xpjuice`)
- Fluids registered: 2 (`xpjuice`, `flowing_xpjuice`)
- Fluid blocks registered: 1 (`xpjuice`)
- Block entity types registered: 35
- Entity types registered: 11
- Sound events registered: 19
- Custom statistics registered: 1 (`open_blocks:bricks_dropped`)
- Enchantments registered: 3
- Menu types registered: 14
- Recipe serializers registered: 10
- Command roots wired: 3 (`flimflam`, `luck`, `ob_inventory`)
- Custom capabilities wired: 3 (`open_blocks:luck`, `open_blocks:pedometer_state`, `open_blocks:bowels`)
- Custom advancement triggers wired: 2 (`open_blocks:brick_dropped`, `open_blocks:dev_null_stacked`)

Current asset coverage:
- Blockstates: `src/main/resources/assets/open_blocks/blockstates` (41 files)
- Block models: `src/main/resources/assets/open_blocks/models/block` (41 files)
- Item models: `src/main/resources/assets/open_blocks/models/item` (84 files)
- Language file: `src/main/resources/assets/open_blocks/lang/en_us.json`
- Sound definitions: `src/main/resources/assets/open_blocks/sounds.json`
- Sound assets: `src/main/resources/assets/open_blocks/sounds` (legacy OpenBlocks `.ogg` set copied)
- Advancement JSON scaffolding:
  - `src/main/resources/data/open_blocks/advancements/oops.json`
  - `src/main/resources/data/open_blocks/advancements/tma2.json`
- Custom recipe placeholder JSONs: `src/main/resources/data/open_blocks/recipes` (7 files)
- Generated block loot tables: `src/generated/resources/data/open_blocks/loot_tables/blocks` (41 files)
- Generated block tags:
  - `src/generated/resources/data/minecraft/tags/blocks/mineable/pickaxe.json`
  - `src/generated/resources/data/minecraft/tags/blocks/needs_stone_tool.json`
  - `src/generated/resources/data/minecraft/tags/blocks/climbable.json`
- Imported legacy textures:
  - `src/main/resources/assets/open_blocks/textures/block`
  - `src/main/resources/assets/open_blocks/textures/item`

Current validation status:
- `./gradlew compileJava` passes.
- `./gradlew compileJava runData` passes.
- `./gradlew runData` now executes `Loot Tables` provider and writes 41 block loot tables.
- `./gradlew runData` now also executes `Tags for minecraft:block mod id open_blocks` and writes 3 baseline block tag files.
- Custom recipe placeholder JSONs for all 7 legacy custom recipe IDs load with current datagen/compile loop.
- Legacy ore-dict compatibility tags now use broader `forge`/`minecraft` groups + fallback items across 34 updated files in `src/main/resources/data/open_blocks/tags/items/legacy_ore_dict`.
- Latest `./gradlew compileJava runData` pass (2026-02-09) still succeeds after adding `OBInventoryEvent` store/load bridge wiring for `/ob_inventory`.
- Latest `./gradlew compileJava runData` pass (2026-02-09) still succeeds after adding grave gamerule + drop-backup dump wiring (`OBGameRules` + `OBGraveHooks`).
- Latest `./gradlew compileJava runData` pass (2026-02-09) still succeeds after deepening grave flow to place+claim baseline (`OBGraveBlock`, `OBGraveBlockEntity`, `OBGraveHooks`, and `OBInventoryStore` grave helpers).
- Latest `./gradlew compileJava runData` pass (2026-02-09) still succeeds after wiring grave drop-action filtering + interaction follow-up (`OBGraveDropsEvent`, updated `OBGraveHooks`, and shovel-gated grave claim/death-message interaction in `OBGraveBlock` + `OBGraveBlockEntity`).
- Latest `./gradlew compileJava runData` pass (2026-02-09) still succeeds after fixing `open_blocks:xpjuice` client fluid texture extensions in `OBFluidTypes` to prevent null-sprite crashes in third-party fluid-item renderers.
- Latest `./gradlew compileJava runData` pass (2026-02-09) still succeeds after starting a first non-GUI block depth sweep (`OBHealBlock`/`OBHealBlockEntity`, `OBPathBlock`, `OBSpongeBlock`, plus `OBBlocks`/`OBBlockEntities` wiring updates).
- Latest `./gradlew compileJava runData` pass (2026-02-09) still succeeds after ladder-domain sweep additions (`OBLadderBlock`, `OBRopeLadderBlock`, and `OBBlocks` wiring) for support/chain/auto-extension behavior.
- Latest `./gradlew compileJava runData` pass (2026-02-09) still succeeds after elevator-domain sweep additions (`OBElevatorBlock`, `OBElevatorRotatingBlock`, `OBBlocks` wiring, and rotating-elevator facing blockstate variants) for baseline bidirectional teleport behavior.
- Capability scaffold note:
  - Legacy player capabilities use the `open_blocks` namespace. Luck transfers on respawn, while the Pedometer route and stored bricks reset on death. The Pedometer also resets on dimension changes.
- Build rule note: `build.gradle` skips optional jars from `extra-mods-1.20.1` when task names include `runData`/`datagen`, so datagen is not blocked by unrelated runtime mods.
- Client asset note: model texture paths must use `open_blocks:block/...` and `open_blocks:item/...` (not plural `blocks/items`) for 1.20 atlas resolution.
- Fluid client-extension note: third-party renderers that query `IClientFluidTypeExtensions` directly require non-null still/flowing texture keys; `OBFluidTypes` now explicitly supplies `open_blocks:block/xp_juice_still` and `open_blocks:block/xp_juice_flowing` for `open_blocks:xpjuice`.
- Run-log note: `run/logs/latest.log` had a concrete missing texture warning for `open_blocks:paint_mixer` (`open_blocks:model/paint_mixer`).
  - Fixed by switching to atlas-safe `open_blocks:block/paint_mixer_model` and adding `textures/block/paint_mixer_model.png`.
- Legacy remap note:
  - `src/main/java/art/arcane/openblocks/registry/OBMissingMappings.java` now handles compatibility remaps for missing mappings.
  - Namespace remap coverage: `openblocks` and `OpenBlocks` to canonical `open_blocks`.
  - Alias coverage includes legacy camelCase/compact IDs and lowercased fallback forms for blocks/items/fluids/block entities/menus.
  - Core aliases include:
    - blocks: `vacuumhopper`, `bigbutton`, `xpbottler`, `autoanvil`, `autoenchantmenttable`, `xpdrain`, `blockbreaker`, `blockPlacer`, `itemDropper`, `ropeladder`, `donationStation`, `paintmixer`, `paintcan`, `canvasglass`, `drawingtable`, `xpshower`, `goldenegg`
    - items: `hangglider`, `sonicglasses`, `pencilGlasses`, `crayonGlasses`, `technicolorGlasses`, `seriousGlasses`, `craneControl`, `craneBackpack`, `filledbucket`, `sleepingBag`, `paintBrush`, `heightMap`, `emptyMap`, `tastyClay`, `goldenEye`, `genericUnstackable`, `infoBook`, `devnull`, `spongeonastick`, `epicEraser`
    - fluid: `liquidxp` -> `xpjuice`
- Systems skeleton note:
  - `src/main/java/art/arcane/openblocks/command/OBCommands.java` now registers the three legacy command IDs in Brigadier form (`flimflam`, `luck`, `ob_inventory`).
  - `src/main/java/art/arcane/openblocks/command/OBInventoryStore.java` now provides the breadth-stage inventory dump backend used by `/ob_inventory`.
  - `src/main/java/art/arcane/openblocks/command/OBInventoryHooks.java` now stores command-consumable inventory dumps on player death.
  - `src/main/java/art/arcane/openblocks/api/OBInventoryEvent.java` now mirrors legacy inventory store/load event semantics for arbitrary subsystem payload IDs.
  - `src/main/java/art/arcane/openblocks/api/OBGraveDropsEvent.java` now mirrors legacy grave drop-action event semantics (`STORE`/`DROP`/`DELETE`) for grave filtering hooks.
  - `src/main/java/art/arcane/openblocks/world/OBGameRules.java` now registers legacy grave gamerule key `openblocks:spawn_graves` (default `true`).
  - `src/main/java/art/arcane/openblocks/block/OBGraveBlock.java` + `src/main/java/art/arcane/openblocks/block/entity/OBGraveBlockEntity.java` now provide placeable grave state with persisted dump metadata, death message display on normal interaction, and shovel-gated loot claim behavior.
  - `OBHealBlockEntity` applies one tick each of Regeneration and Saturation every 20 server ticks to nearby non-creative players.
  - `OBElevatorBlock` and `OBElevatorRotatingBlock` share a dye color blockstate. Rotating elevators store their facing in blockstate and set yaw on arrival.
  - `OBElevatorTravel` handles jump/sneak requests from `OBElevatorControls` and `OBNetwork`. It validates the player's position and destination on the server. The world server config controls range, block rules, overrides, centering, clearance and XP costs.
  - The item uses vanilla `BlockStateTag.color` and the generated loot tables preserve color. Untagged items and old block states default to white. One color-preserving rotating upgrade replaces the 16 metadata-specific upgrades.
  - `OBLadderBlock` is the Jaded Ladder trapdoor. It climbs when open and uses vanilla trapdoor placement, redstone, sounds, and waterlogging.
  - `OBRopeLadderBlock` stores facing and drop eligibility in blockstate. Ropes extend downward and detach below a broken rung. Finite ropes consume one item per rung. Infinite ropes charge only the anchor and never yield drops from their rungs.
  - `OBScaffoldingBlock` and `OBScaffoldingItem` restore solid temporary scaffolds that drop when they decay and burn for 100 ticks.
  - `OBPathBlock` is a decorative, non-colliding path with a sturdy underside. It drops when its support is removed. `OBPathGeometry` bakes the legacy scattered stones from a stable position seed and uses a fixed seed for the item model.
  - `OBSpongeBlock` clears liquids periodically, burns on lava contact, and schedules adjacent fluids to refill the cleared area on removal when neighbor notifications are disabled.
  - `OBSpongeOnAStickItem` clears liquids on block or air use and spends one durability when it absorbs fluid. Lava destroys the tool and sets the player on fire. Both sponge forms use `OBSpongeDrain` to preserve waterlogged blocks and have separate radius and block-update settings in the world server config.
  - `OBTankBlockEntity` stores fluid per block and exposes a column-wide Forge fluid capability. It fills upwards from the connection and drains compatible tanks above it first. Tanks settle downwards and balance horizontally when enabled. Fluid identity and amounts survive storage, loot drops, portable fluid transfers, and placement.
  - `OBTankBlock` supports bucket transfer, next-level XP withdrawal, comparators, fluid lighting, and filled pick-block. `OBTankGeometry` culls shared frame edges. `OBTankRenderer` shows amounts and joins compatible fluid surfaces. `OBTankItem` exposes a fluid handler and shows each tank's contents.
  - `OBLiquidExperience` converts Liquid XP and configured alternative fluids. The defaults are 16 buckets per tank and 20 mB per XP point. Configure other fluids with entries such as `modid:fluid=20` in the world server config.
  - `OBLiquidExperienceBlock` restores `xpBucket.directFill` for world sources, independently of the tank's drain setting. `OBCreativeTabs` uses `xpBucket.showInCreativeGui` for the Open Blocks tab and creative search. The registered Forge XP Bucket also serves as the fluid's standard container.
  - `OBXPDrainBlockEntity` converts up to four player XP points per tick and collects XP orbs over its grate through the upper fluid face of the block below. It honors the Forge XP event and preserves the constituents of a merged orb that cannot fit.
  - `OBXPShowerBlock` mounts horizontally and responds to redstone. Its block entity pulls up to 100 mB every three ticks from behind the nozzle, saves the buffer, and releases falling orbs and Liquid XP particles. The `xp_orb_no_fly` entity uses the vanilla XP renderer, pickup, and gravity without acquiring a player to follow.
  - `OBXPBottlerBlockEntity` uses one glass bottle and eight XP points per cycle and restores the bottler sound. It exposes configured Forge item and fluid faces, pulls up to 10 mB per tick, and can pull empty bottles and eject output. Side selection and automatic transfers are separate settings and start disabled. Inventory, fluid, progress, and side settings are stored in the world.
  - `OBXPBottlerBlock` restores the directional case textures. `OBXPBottlerMenu` syncs the bottle slots, full-width fluid data, progress, and automation settings via the vanilla container protocol. `OBXPBottlerScreen` provides the fluid gauge and world-side controls in a panel next to the inventory.
  - `OBVacuumHopperBlockEntity` draws in loose items and XP orbs and stores up to ten stacks and five levels of Liquid XP. It preserves partial pickups, routes up to 50 mB and one successful stack transfer per ten ticks, and exposes extract-only capabilities on configured faces. Empty-hand sneak-use pauses suction and automatic output while preserving direct contact pickup.
  - `OBVacuumHopperMenu` and `OBVacuumHopperScreen` configure item and XP faces independently and show storage and pause state. `OBVacuumHopperGeometry` adds the legacy item, fluid, and combined nozzles from the synchronized output masks. Suction ignores rising Cannon shots and can collect them as they descend.
  - `OBRedstoneMachineBlock` restores the six directions, piston sounds, and active breaker texture. Its block entity harvests into a rear inventory or leaves drops at the target, places the first available item into a replaceable target, and ejects one item per rising edge with optional analog speed scaling. Break and placement use a Forge fake player and normal interaction hooks.
  - `OBRedstoneMachineMenu` and `OBRedstoneMachineScreen` provide the two nine-slot inventories and the Dropper's speed and scaling controls. The server validates settings against the current menu and world config. The recipes and established block, item, block entity, and menu IDs are preserved.
  - `OBXpWorkbenchBlockEntity` stores the Auto Anvil and Auto Enchantment Table inputs, outputs, Liquid XP, and four independently configured ports. The anvil calculates results through the Forge anvil menu and spends the point equivalent of its full level cost. The enchanting table checks the offer level and spends the XP difference and lapis for the chosen tier. It uses the modern Forge bookshelf and enchanting hooks.
  - `OBXpWorkbenchMenu` and `OBXpWorkbenchScreen` restore the inventories, gauges, and port controls. The enchanting table has a three-tier selector, a bookshelf power limit and a seeded offer. `OBXpWorkbenchBlock` rotates the anvil's two axes and restores bookshelf particles. `OBXpWorkbenchRendering` delegates to the vanilla animated book renderer.
  - `OBSprinklerBlockEntity` pulls water from below and accepts water and bone meal through Forge. It preserves paid spray time, saves fertilizer and water, and uses the Forge bonemeal hook for the legacy growth range. `OBSprinklerRendering` restores the moving nozzle and the particle settings.
  - `OBDonationStationBlockEntity` preserves the one-slot sample inventory. `OBDonationMetadata` reads the registry owner's name, authors, and links, including the `donateUrl` IMC method. `OBComputerCraftDonations` provides `getItemAuthor` through optional CC: Tweaked. The original piggy-bank model rotates in all four horizontal directions.
  - `OBCannonBlockEntity` pulls a stack from an adjacent Forge inventory once per second while powered. `OBPointerItem` selects a Cannon and aims it at the top center of a clicked block. `OBItemProjectile` keeps vanilla pickup, collision, and fluid behavior with drag-free flight in air. `OBCannonRendering` restores the model, recoil, and the Pointer's flight preview. `OBComputerCraftCannon` exposes yaw, pitch, and speed through optional CC: Tweaked.
  - `OBBigButtonBlockEntity` holds eight stacks that set the duration of a full-strength pulse. `OBBigButtonBlock` attaches to all six faces, notifies its support and neighbors, and lets arrows hold the wooden plate. `OBBigButtonMenu` and `OBBigButtonScreen` restore the timing inventory and its live readout.
  - `OBFoldingTargetBlock` raises its face on incoming redstone and scores the exact arrow collision. `OBFoldingTargetBlockEntity` provides the ten-tick signal and returns the target's embedded, collectible arrows on folding. The optional Flan's Mod projectile integration remains open.
  - `OBFanBlockEntity` saves a continuous heading and applies distance-scaled horizontal airflow in a cone. Incoming signal controls force and blade speed. `OBBearTrapBlockEntity` captures eligible mobs, holds them at its base, releases on use or redstone, and exposes a comparator signal. `OBFloorDeviceRendering` animates both blocks from their synchronized state and renders their models as items.
  - `OBFlagBlock` restores floor, wall, fence, and stacked mounting, and `OBFlagRendering` uses each block's saved color and bearing. `OBSkyRendering` composites the live sky from the Forge render stages with ordinary depth testing. `OBDecorationChunkUpgrade` restores missing block entities for those two blocks in existing 1.20.1 chunks.
  - `src/main/java/art/arcane/openblocks/grave/OBGraveHooks.java` now writes grave backup dumps, posts/consumes `OBGraveDropsEvent` action filtering, searches for valid placement nearby, places grave blocks on successful death handling, and clears only stored grave drops when grave placement succeeds.
  - `src/main/java/art/arcane/openblocks/command/OBFlimFlamEffects.java` now provides executable breadth-stage actions for all legacy `/flimflam` effect IDs.
  - Current command parity:
    - `luck` reads/writes `open_blocks:luck` capability state.
    - `flimflam` now executes a 17-effect action map with legacy ID names preserved; deeper legacy parity (luck/cost/weight/blacklist semantics and exact per-effect behavior) remains pending.
    - `ob_inventory` now supports main + built-in sub-inventory dump/restore/spawn:
      - dumps written to `<world>/data/inventory-*.dat`,
      - restore loads stored main inventory onto target player,
      - restore also reapplies serialized ender chest payload when present,
      - spawn drops target inventory stacks (or selected slot) at command source for `main`, `armor`, `offhand`, and `ender_chest`,
      - store/restore now also round-trip arbitrary sub-inventory payload maps through `OBInventoryEvent.Store`/`OBInventoryEvent.Load`,
      - dump root now includes player `Location` metadata (`X`,`Y`,`Z`,`Dimension`) for future grave/backend integrations.
      - grave backup dump writes/reads/deletes are now available via `OBInventoryStore.storeDroppedItems(...)`, `OBInventoryStore.readDroppedItems(...)`, and `OBInventoryStore.deleteDump(...)` and are consumed by the grave place/claim flow.
    - legacy arbitrary subsystem payload consumers/producers remain pending; grave parity now includes baseline drop-action filtering, but still lacks richer metadata/placement depth and concrete external grave event integrations.
  - `/luck` state storage now reads/writes through `OBCapabilities` (`open_blocks:luck`) rather than ad-hoc player persistent data.
  - `OBCapabilities` registers `open_blocks:luck`, `open_blocks:pedometer_state`, and `open_blocks:bowels`. It carries luck through clones, preserves stored bricks on non-death clones, and preserves a Pedometer route on non-death clones within the same dimension.
  - `OBCapabilities.PedometerState` keeps the sampled route and report data. The player capability provider restores access to the same data when Forge revives capabilities after a dimension change or during a respawn copy.
  - `src/main/java/art/arcane/openblocks/advancement/OBCriterions.java` now registers custom trigger IDs `open_blocks:brick_dropped` and `open_blocks:dev_null_stacked` for advancement compatibility baseline.
  - `OBBrickActions` handles Tasty Clay digestion, the Boo packet, and death drops when the server's `tomfoolery.weAreSeriousPeople` option is false. Only a successfully spawned Boo brick advances `brick_dropped` and the Bricks dropped statistic.
  - `OBPedometerTracking` samples a running trip once per server tick while a Pedometer is in the hotbar or offhand. Reports use recorded samples and advance the last-check marker.
  - `OBDevNullMenu` fires `dev_null_stacked` from the stored item's actual nesting depth after inventory edits. `OBDevNullPickup` fills exact matching stacks on pickup before discarding overflow.
  - `src/main/java/art/arcane/openblocks/registry/OBItems.java` now sets parity-critical item properties:
    - `tasty_clay` is edible,
    - `dev_null` and `generic_unstackable` are stack size 1.
    - `pedometer` is stack size 1.
  - `src/main/java/art/arcane/openblocks/item/OBPedometerItem.java` now provides explicit pedometer interaction flow:
    - right-click starts tracking,
    - sneak-right-click resets tracking,
    - right-click while running prints report lines using legacy pedometer lang keys.

## Top-Level Entry Points
- Mod entrypoint and major registration flow:
  - `old-1.12.2/src/main/java/openblocks/OpenBlocks.java`
- Config-driven registration + enchantments + meta items:
  - `old-1.12.2/src/main/java/openblocks/Config.java`
- Custom recipe registry event:
  - `old-1.12.2/src/main/java/openblocks/CustomRecipesSetup.java`
- GUI handler (luggage + dev null):
  - `old-1.12.2/src/main/java/openblocks/OpenBlocksGuiHandler.java`
- API provider bootstrap:
  - `old-1.12.2/src/main/java/openblocks/ApiSetup.java`

## Registry Surface Summary
- `@RegisterBlock` entries: 41
- `@RegisterItem` entries: 30
- Primary fluid(s): 1 (`xpjuice`)
- Enchantments: 3
- Sound events registered in code: 19
- Mod entities registered in code: 11
- Block entities referenced by block registration: 35
- Custom code-registered recipes: 7
- Commands: 3
- Custom advancements/triggers: 2
- Custom capabilities: 3

## Legacy Recipe Corpus Snapshot
Source: `old-1.12.2/src/main/resources/assets/openblocks/recipes`

- Total recipe files: 189
- Top-level recipe types:
  - `forge:ore_shaped`: 107
  - `forge:ore_shapeless`: 78
  - `openmods:enchanting`: 4
- Existing 1.20 scaffold recipes currently present:
  - `src/main/resources/data/open_blocks/recipes` (7 custom serializer placeholders from `CustomRecipesSetup` + 1 custom flim-flam replacement recipe)
- Mechanical conversion output now present:
  - `src/main/resources/data/open_blocks/recipes/legacy` (170 recipes, including the single upgrade that covers all elevator colors)
  - `src/main/resources/data/open_blocks/tags/items/legacy_ore_dict` (35 compatibility item-tag files for former ore-dict names)
  - Converted types:
    - `forge:ore_shaped` -> `minecraft:crafting_shaped`
    - `forge:ore_shapeless` -> `minecraft:crafting_shapeless`
  - Conversion baseline details:
    - legacy `openblocks:*` IDs remapped to `open_blocks:*`
    - vanilla metadata resolves to flattened item IDs. Legacy generic components and the pointer now use distinct IDs. Color and state variants of other OpenBlocks items still need migration.
    - ore dictionary entries translated to `open_blocks:legacy_ore_dict/*` tags (now populated with broader `forge`/`minecraft` group tags plus vanilla fallback items)
  - Legacy `openmods:enchanting` replacements:
    - `open_blocks:flim_flam_book` custom crafting recipe implemented in:
      - `src/main/java/art/arcane/openblocks/recipe/OBFlimFlamBookRecipe.java`
      - `src/main/resources/data/open_blocks/recipes/flim_flam_book.json`
- Known conversion blockers (breadth-pass):
  - Ore dictionary ingredient conversion (`forge:ore_dict` -> modern tags).
  - Metadata-based result variants (`data` field) for color/state/meta-item fidelity.
  - Metadata/meta-item fidelity for converted legacy recipes still needs a depth pass.

## Blocks (`OpenBlocks.Blocks`)
Source: `old-1.12.2/src/main/java/openblocks/OpenBlocks.java`

| ID | Block Class | Field | Tile Entity | ItemBlock / Item Wrapper | Legacy IDs / Notes |
|---|---|---|---|---|---|
| ladder | BlockLadder | `Blocks.ladder` | - | default | - |
| guide | BlockGuide | `Blocks.guide` | TileEntityGuide | ItemGuide | - |
| builder_guide | BlockBuilderGuide | `Blocks.builderGuide` | TileEntityBuilderGuide | ItemGuide | - |
| elevator | BlockElevator | `Blocks.elevator` | - | ItemElevator | `registerDefaultItemModel = false` |
| elevator_rotating | BlockElevatorRotating | `Blocks.elevatorRotating` | TileEntityElevatorRotating | ItemElevator | `registerDefaultItemModel = false` |
| heal | BlockHeal | `Blocks.heal` | TileEntityHealBlock | default | - |
| target | BlockTarget | `Blocks.target` | TileEntityTarget | default | - |
| grave | BlockGrave | `Blocks.grave` | TileEntityGrave | default | - |
| flag | BlockFlag | `Blocks.flag` | TileEntityFlag | ItemFlagBlock | 1.20.1 uses `OBFlagBlock`, `OBFlagBlockEntity`, and `OBFlagItem`. Wall facing and cloth color are block properties. Upright yaw is synced block-entity data. |
| tank | BlockTank | `Blocks.tank` | TileEntityTank | ItemTankBlock | - |
| trophy | BlockTrophy | `Blocks.trophy` | TileEntityTrophy | ItemTrophyBlock | - |
| beartrap | BlockBearTrap | `Blocks.bearTrap` | TileEntityBearTrap | default | - |
| sprinkler | BlockSprinkler | `Blocks.sprinkler` | TileEntitySprinkler | default | - |
| cannon | BlockCannon | `Blocks.cannon` | TileEntityCannon | default | `OpenBlock` TESR path |
| vacuum_hopper | BlockVacuumHopper | `Blocks.vacuumHopper` | TileEntityVacuumHopper | default | `vacuumhopper` |
| sponge | BlockSponge | `Blocks.sponge` | - | default | - |
| big_button | BlockBigButton | `Blocks.bigButton` | TileEntityBigButton | default | `bigbutton` |
| big_button_wood | BlockBigButtonWood | `Blocks.bigButtonWood` | TileEntityBigButtonWood | default | - |
| imaginary | BlockImaginary | `Blocks.imaginary` | TileEntityImaginary | ItemImaginary | custom item models; no default model |
| fan | BlockFan | `Blocks.fan` | TileEntityFan | default | - |
| xp_bottler | BlockXPBottler | `Blocks.xpBottler` | TileEntityXPBottler | default | `xpbottler` |
| village_highlighter | BlockVillageHighlighter | `Blocks.villageHighlighter` | TileEntityVillageHighlighter | default | - |
| path | BlockPath | `Blocks.path` | - | default | - |
| auto_anvil | BlockAutoAnvil | `Blocks.autoAnvil` | TileEntityAutoAnvil | default | `autoanvil` |
| auto_enchantment_table | BlockAutoEnchantmentTable | `Blocks.autoEnchantmentTable` | TileEntityAutoEnchantmentTable | default | `autoenchantmenttable` |
| xp_drain | BlockXPDrain | `Blocks.xpDrain` | TileEntityXPDrain | default | `xpdrain` |
| block_breaker | BlockBlockBreaker | `Blocks.blockBreaker` | TileEntityBlockBreaker | default | `blockbreaker` |
| block_placer | BlockBlockPlacer | `Blocks.blockPlacer` | TileEntityBlockPlacer | default | `blockPlacer` |
| item_dropper | BlockItemDropper | `Blocks.itemDropper` | TileEntityItemDropper | default | `itemDropper` |
| rope_ladder | BlockRopeLadder | `Blocks.ropeLadder` | - | default | `ropeladder` |
| donation_station | BlockDonationStation | `Blocks.donationStation` | TileEntityDonationStation | default | `donationStation` |
| paint_mixer | BlockPaintMixer | `Blocks.paintMixer` | TileEntityPaintMixer | default | `paintmixer` |
| canvas | BlockCanvas | `Blocks.canvas` | TileEntityCanvas | default | - |
| paint_can | BlockPaintCan | `Blocks.paintCan` | TileEntityPaintCan | ItemPaintCan | `paintcan` |
| canvas_glass | BlockCanvasGlass | `Blocks.canvasGlass` | TileEntityCanvasGlass | default | `canvasglass` |
| projector | BlockProjector | `Blocks.projector` | TileEntityProjector | default | - |
| drawing_table | BlockDrawingTable | `Blocks.drawingTable` | TileEntityDrawingTable | default | `drawingtable` |
| sky | BlockSky | `Blocks.sky` | TileEntitySky | ItemSkyBlock | 1.20.1 uses `OBSkyBlock`, `OBSkyBlockEntity`, and `OBSkyItem`. The original ID carries both polarities. `OBSkyRendering` uses Forge render stages to display the current dimension's sky. |
| xp_shower | BlockXPShower | `Blocks.xpShower` | TileEntityXPShower | default | `xpshower` |
| golden_egg | BlockGoldenEgg | `Blocks.goldenEgg` | TileEntityGoldenEgg | default | `goldenegg` |
| scaffolding | BlockScaffolding | `Blocks.scaffolding` | - | BlockScaffolding.Item | - |

## Items (`OpenBlocks.Items`)
Source: `old-1.12.2/src/main/java/openblocks/OpenBlocks.java`

| ID | Item Class | Field | Legacy IDs / Notes |
|---|---|---|---|
| hang_glider | ItemHangGlider | `Items.hangGlider` | `hangglider` |
| generic | ItemOBGeneric | `Items.generic` | no default model; meta item holder |
| luggage | ItemLuggage | `Items.luggage` | - |
| sonic_glasses | ItemSonicGlasses | `Items.sonicGlasses` | `sonicglasses` |
| pencil_glasses | ItemImaginationGlasses | `Items.pencilGlasses` | `pencilGlasses`, unlocalized `glasses.pencil` |
| crayon_glasses | ItemCrayonGlasses | `Items.crayonGlasses` | `crayonGlasses`, unlocalized `glasses.crayon` |
| technicolor_glasses | ItemImaginationGlasses | `Items.technicolorGlasses` | `technicolorGlasses`, unlocalized `glasses.technicolor` |
| serious_glasses | ItemImaginationGlasses | `Items.seriousGlasses` | `seriousGlasses`, unlocalized `glasses.admin` |
| crane_control | ItemCraneControl | `Items.craneControl` | `craneControl` |
| crane_backpack | ItemCraneBackpack | `Items.craneBackpack` | `craneBackpack` |
| slimalyzer | ItemSlimalyzer | `Items.slimalyzer` | - |
| xp_bucket | ItemXpBucket | `Items.xpBucket` | `filledbucket` |
| sleeping_bag | ItemSleepingBag | `Items.sleepingBag` | `sleepingBag` |
| paintbrush | ItemPaintBrush | `Items.paintBrush` | `paintBrush` |
| stencil | ItemStencil | `Items.stencil` | no default model |
| squeegee | ItemSqueegee | `Items.squeegee` | - |
| height_map | ItemHeightMap | `Items.heightMap` | `heightMap` |
| empty_map | ItemEmptyMap | `Items.emptyMap` | `emptyMap` |
| cartographer | ItemCartographer | `Items.cartographer` | - |
| tasty_clay | ItemTastyClay | `Items.tastyClay` | `tastyClay`. In 1.20.1, `OBBrickActions` adds a stored brick after the always-edible food is consumed with digestion enabled. |
| golden_eye | ItemGoldenEye | `Items.goldenEye` | `goldenEye` |
| generic_unstackable | ItemOBGenericUnstackable | `Items.genericUnstackable` | no default model; `genericUnstackable` |
| cursor | ItemCursor | `Items.cursor` | - |
| info_book | ItemInfoBook | `Items.infoBook` | `infoBook` |
| dev_null | ItemDevNull | `Items.devNull` | `devnull`. The 1.20.1 port uses `OBDevNullItem`, with one stored stack in item NBT. |
| sponge_on_a_stick | ItemSpongeOnAStick | `Items.spongeonastick` | `spongeonastick` |
| pedometer | ItemPedometer | `Items.pedometer` | `OBPedometerItem` restores start, report, and sneak-reset in both hands. The `open_blocks:speed` predicate switches to the original moving model at 0.1 blocks per tick. |
| epic_eraser | ItemEpicEraser | `Items.epicEraser` | `epicEraser` |
| wrench | ItemWrench | `Items.wrench` | - |
| glyph | ItemGlyph | `Items.glyph` | - |

## Meta Item Registries
Source:
- `old-1.12.2/src/main/java/openblocks/common/item/MetasGeneric.java`
- `old-1.12.2/src/main/java/openblocks/common/item/MetasGenericUnstackable.java`

- `Items.generic` (`ItemOBGeneric`) variants:
  - `glider_wing`
  - `beam`
  - `crane_engine`
  - `crane_magnet`
  - `miracle_magnet` (conditional: OpenPeripheralCore + config)
  - `line`
  - `map_controller`
  - `map_memory`
  - `cursor` (deprecated placeholder, disabled)
  - `assistant_base`
  - `unprepared_stencil`
  - `sketching_pencil`
- `Items.genericUnstackable` (`ItemOBGenericUnstackable`) variants:
  - `pointer`

The 1.20.1 port registers the listed active variants as distinct `open_blocks` items and routes converted recipe inputs and outputs to those IDs. The old `generic` and `generic_unstackable` IDs remain loadable for existing worlds. Old generic stacks contain no subtype to recover. The old unstackable ID has a conversion recipe for its only variant, the pointer. Pointer behavior and the Miracle Magnet's conditional integration are tracked with their feature ports.

## Legacy Creative Tab Behavior (Model/Lang Parity Critical)
Primary sources:
- `old-1.12.2/src/main/java/openblocks/OpenBlocks.java` (`createTabOpenBlocks`, `setupConfigPre`)
- Item/block classes with `getSubItems` overrides under `old-1.12.2/src/main/java/openblocks/common/item`

Core behavior:
- `setupConfigPre` sets one shared creative tab for registered OpenBlocks content: `gameConfig.setCreativeTab(OpenBlocks::createTabOpenBlocks)`.
- Tab icon is `Blocks.flag` with default color, fallback `minecraft:sponge` if flag is disabled/unavailable.
- Tab appends enchanted books for `explosive`, `last_stand`, and `flim_flam` when those enchantments are registered.

Important item variant behavior in legacy tab:
- `ItemElevator`: exposes 16 color metadata variants.
- `ItemPaintCan`: exposes one full can per color.
- `ItemSkyBlock`: exposes normal + inverted variants. The port represents inversion with `BlockStateTag.inverted` and restores torch-based conversion in both directions.
- `ItemFlagBlock`: the tab shows a lime flag and uses it as the icon. All sixteen cloth colors are available by carpet crafting or cycling the held stack. The port preserves the color using `BlockStateTag.color`.
- `ItemTrophyBlock`: exposes one stack per trophy/entity type.
- `ItemImaginary`: exposes pencil + all crayon colors.
- `ItemPaintBrush`: exposes blank + colored variants.
- `ItemStencil`: exposes all stencil patterns.
- `ItemEmptyMap`: exposes scale variants (`0..3`).
- `ItemGoldenEye`: exposes charged and depleted states.
- `ItemGlyph`: exposes display character set in OpenBlocks tab; optionally full charset in Search tab (config-controlled).
- `ItemImaginationGlasses.ItemCrayonGlasses`: exposes one pair per color.
- `ItemCartographer`: exposes all assistant type variants (currently one enum value).
- `ItemXpBucket`: only appears when `Config.showXpBucketInCreative` is true.
- `ItemHeightMap`: intentionally hidden from creative tab (`getSubItems` is empty).

Port implication:
- The 1.20.1 tab lists distinct components and omits the two deprecated generic IDs. Most stateful items still show only their default stack.
- Metadata/NBT creative-variant parity is a later depth pass item.

## Fluids
Source: `old-1.12.2/src/main/java/openblocks/OpenBlocks.java`

- Registered fluid object:
  - ID: `xpjuice`
  - Field: `OpenBlocks.Fluids.xpJuice`
  - Texture RLs: `openblocks:blocks/xp_juice_still`, `openblocks:blocks/xp_juice_flowing`
- Registration occurs in `OpenBlocks.preInit`:
  - `FluidRegistry.registerFluid(Fluids.xpJuice)`
  - optional `FluidRegistry.addBucketForFluid(Fluids.xpJuice)` when enabled
- XP conversion system:
  - `old-1.12.2/src/main/java/openblocks/common/FluidXpUtils.java`
  - supports additional fluid mappings from config (`Config.additionalXpFluids`)
- Note: `assets/openblocks/blockstates/xp_bucket.json` references `liquidxp` string.

## Enchantments
Source: `old-1.12.2/src/main/java/openblocks/Config.java`

- Registered enchantments:
  - `openblocks:explosive` (`EnchantmentExplosive`)
  - `openblocks:last_stand` (`EnchantmentLastStand`)
  - `openblocks:flim_flam` (`EnchantmentFlimFlam`)
- Object holders:
  - `OpenBlocks.Enchantments` in `OpenBlocks.java`

## Sounds
Source:
- Registration code: `old-1.12.2/src/main/java/openblocks/OpenBlocks.java`
- Definitions: `old-1.12.2/src/main/resources/assets/openblocks/sounds.json`

Registered in code (19):
- `elevator.activate`
- `grave.rob`
- `crayon.place`
- `luggage.walk`
- `luggage.eat.food`
- `luggage.eat.item`
- `pedometer.use`
- `slimalyzer.signal`
- `squeegee.use`
- `best.feature.ever.fart`
- `annoying.mosquito`
- `annoying.alarmclock`
- `annoying.vibrate`
- `beartrap.open`
- `beartrap.close`
- `cannon.activate`
- `target.open`
- `target.close`
- `bottler.signal`

Additional keys present in `sounds.json` but not registered in code:
- `radio.activate`
- `unused.mortar`

## Entities
Source: `old-1.12.2/src/main/java/openblocks/OpenBlocks.java`

Registered mod entities:
- `openblocks:luggage` -> `EntityLuggage` (legacy numeric ID constant `702`)
- `openblocks:xp_orb_no_fly` -> `EntityXPOrbNoFly` (`709`)
- `openblocks:hang_glider` -> `EntityHangGlider` (`701`)
- `openblocks:magnet` -> `EntityMagnet` (`703`)
- `openblocks:mounted_block` -> `EntityMountedBlock` (`704`)
- `openblocks:player_magnet` -> `EntityMagnet.PlayerBound` (`708`)
- `openblocks:cartographer` -> `EntityCartographer` (`705`)
- `openblocks:item_projectile` -> `EntityItemProjectile` (`706`)
- `openblocks:golden_eye` -> `EntityGoldenEye` (`707`)
- `openblocks:mini_me` -> `EntityMiniMe` (`710`)
- `openblocks:glyph` -> `EntityGlyph` (`711`)

Entity package inventory:
- `old-1.12.2/src/main/java/openblocks/common/entity`
- Not directly registered as mod entities but present as helpers/base classes:
  - `EntityAssistant`
  - `EntityMount`
  - `EntitySmoothMove`

## Block Entities (Tile Entities)
Source package:
- `old-1.12.2/src/main/java/openblocks/common/tileentity`

Registered through block annotations (35 references):
- TileEntityGuide
- TileEntityBuilderGuide
- TileEntityElevatorRotating
- TileEntityHealBlock
- TileEntityTarget
- TileEntityGrave
- TileEntityFlag
- TileEntityTank
- TileEntityTrophy
- TileEntityBearTrap
- TileEntitySprinkler
- TileEntityCannon
- TileEntityVacuumHopper
- TileEntityBigButton
- TileEntityBigButtonWood
- TileEntityImaginary
- TileEntityFan
- TileEntityXPBottler
- TileEntityVillageHighlighter
- TileEntityAutoAnvil
- TileEntityAutoEnchantmentTable
- TileEntityXPDrain
- TileEntityBlockBreaker
- TileEntityBlockPlacer
- TileEntityItemDropper
- TileEntityDonationStation
- TileEntityPaintMixer
- TileEntityCanvas
- TileEntityPaintCan
- TileEntityCanvasGlass
- TileEntityProjector
- TileEntityDrawingTable
- TileEntitySky
- TileEntityXPShower
- TileEntityGoldenEgg

Helper/base tile entity class in package (not directly registered from block annotations):
- `TileEntityBlockManipulator`

## Containers and GUIs
Primary GUI source files:
- `old-1.12.2/src/main/java/openblocks/OpenBlocksGuiHandler.java`
- `old-1.12.2/src/main/java/openblocks/common/container/*`
- `old-1.12.2/src/main/java/openblocks/client/gui/*`

Non-tile GUI IDs via `OpenBlocksGuiHandler.GuiId`:
- `luggage` -> `ContainerLuggage` / `GuiLuggage`
- `devNull` -> `ContainerDevNull` / `GuiDevNull`. The 1.20.1 port keeps the `open_blocks:dev_null` menu ID with `OBDevNullMenu` and `OBDevNullScreen`.

Tile-based container/gui providers (via `IHasGui` in tile entities):
- TileEntityAutoEnchantmentTable -> ContainerAutoEnchantmentTable / GuiAutoEnchantmentTable
- TileEntityDonationStation -> ContainerDonationStation / GuiDonationStation
- TileEntityProjector -> ContainerProjector / GuiProjector
- TileEntityDrawingTable -> ContainerDrawingTable / GuiDrawingTable
- TileEntityBigButton -> ContainerBigButton / GuiBigButton
- TileEntityItemDropper -> ContainerItemDropper / GuiItemDropper
- TileEntitySprinkler -> ContainerSprinkler / GuiSprinkler
- TileEntityVacuumHopper -> ContainerVacuumHopper / GuiVacuumHopper
- TileEntityBlockPlacer -> ContainerBlockPlacer / GuiBlockPlacer
- TileEntityAutoAnvil -> ContainerAutoAnvil / GuiAutoAnvil
- TileEntityXPBottler -> ContainerXPBottler / GuiXPBottler
- TileEntityPaintMixer -> ContainerPaintMixer / GuiPaintMixer

## Recipes
Custom code-registered recipes:
- Source: `old-1.12.2/src/main/java/openblocks/CustomRecipesSetup.java`
- IDs:
  - `openblocks:crayon_merge`
  - `openblocks:crayon_mix`
  - `openblocks:crayon_glasses`
  - `openblocks:map_clone`
  - `openblocks:map_resize`
  - `openblocks:golden_eye_recharge`
  - `openblocks:epic_eraser_action`

Data recipes:
- Folder: `old-1.12.2/src/main/resources/assets/openblocks/recipes`
- File count: 189 JSON recipes

## Networking, RPC, Sync Registries
Source: `old-1.12.2/src/main/java/openblocks/OpenBlocks.java`

Custom sync type registry (`RegistryEvent.Register<SyncableObjectType>`):
- `EntityCartographer.MapJobs`
- `SyncableBlockLayers`

RPC method registry (`RegistryEvent.Register<MethodEntry>`):
- `IRotatable`
- `IStencilCrafter`
- `IColorChanger`
- `ILevelChanger`
- `ITriggerable`
- `IGuideAnimationTrigger`
- `IItemDropper`

Network event registry (`RegistryEvent.Register<NetworkEventEntry>`):
- `MapDataManager.MapDataRequestEvent`
- `MapDataManager.MapDataResponseEvent`
- `MapDataManager.MapUpdatesEvent`
- `ElevatorActionEvent`
- `PlayerActionEvent`
- `GuideActionEvent`
- `EntityMiniMe.OwnerChangeEvent`

## Capabilities
Sources:
- `old-1.12.2/src/main/java/openblocks/rubbish/BrickManager.java`
- `old-1.12.2/src/main/java/openblocks/common/PedometerHandler.java`
- `old-1.12.2/src/main/java/openblocks/enchantments/FlimFlamEnchantmentsHandler.java`

Registered capability sets:
- `openblocks:bowels` -> `BrickManager.BowelContents`
- `openblocks:pedometer_state` -> `PedometerHandler.PedometerState`
- `openblocks:luck` -> `FlimFlamEnchantmentsHandler.Luck`

## Advancements and Triggers
Sources:
- Trigger registration: `old-1.12.2/src/main/java/openblocks/advancements/Criterions.java`
- Trigger classes:
  - `TriggerDevNullStack` -> trigger ID `openblocks:dev_null_stacked`
  - `TriggerBrickDropped` -> trigger ID `openblocks:brick_dropped`. The 1.20.1 `open_blocks:brick_dropped` trigger is fired by `OBBrickActions` after Be Silly spawns a brick.
- Advancement JSON files:
  - `old-1.12.2/src/main/resources/assets/openblocks/advancements/oops.json`
  - `old-1.12.2/src/main/resources/assets/openblocks/advancements/tma2.json`

## Loot Injection
Source:
- Code: `old-1.12.2/src/main/java/openblocks/common/LootHandler.java`
- Data: `old-1.12.2/src/main/resources/assets/openblocks/loot_tables/inject/technicolor_glasses.json`

Injection behavior:
- Registers loot table `openblocks:inject/technicolor_glasses`
- Injects into:
  - `minecraft:chests/abandoned_mineshaft`
  - `minecraft:chests/simple_dungeon`

## Other Registries and Global Hooks
- Villager profession registry:
  - Source: `old-1.12.2/src/main/java/openblocks/common/RadioVillagerTrades.java`
  - Registers profession `openblocks:radio`, career `audiophile`
- Ore dictionary:
  - Source: `old-1.12.2/src/main/java/openblocks/OpenBlocks.java`
  - `craftingTableWood`
  - `chestWood`
- Game rule registration:
  - Source: `old-1.12.2/src/main/java/openblocks/common/GameRuleManager.java`
  - Adds `openblocks:spawn_graves`
- Commands (server start):
  - `flimflam` (`CommandFlimFlam`)
  - `luck` (`CommandLuck`)
  - `ob_inventory` (`CommandInventory`)

## Client-Side Registration Hotspots
- Main client proxy:
  - `old-1.12.2/src/main/java/openblocks/client/ClientProxy.java`
- Block/item color handlers:
  - `old-1.12.2/src/main/java/openblocks/client/BlockColorHandlerRegistration.java`
- TESR bindings, entity renderers, custom model loader IDs (`magic-devnull`, `magic-path`, `magic-stencil`, `magic-canvas`, `magic-glyph`) in ClientProxy.

## Assets Structure Snapshot
- `assets/openblocks/blockstates` -> 48 JSON
- `assets/openblocks/models/item` -> 59 JSON
- `assets/openblocks/recipes` -> 189 JSON
- `assets/openblocks/sounds.json` + sound OGG files in `assets/openblocks/sounds`
- `assets/openblocks/loot_tables/inject` -> loot injection data
- `assets/openblocks/advancements` -> advancement JSONs

## Notable Unused/Inactive or Special Cases
- `old-1.12.2/src/main/resources/assets/openblocks/blockstates/temp.json` exists and appears unused.
- Integration modules are present but registration calls are commented out in `OpenBlocks.preInit`:
  - `ModuleAdapters`
  - `ModuleTurtles`
- `old-1.12.2/OpenModsLib` submodule contents are absent in this snapshot (folder is empty).
