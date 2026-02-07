# PORTING_MANIFEST.md

## Purpose
- Canonical breadth-first inventory of registries, IDs, and source locations in the 1.12.2 codebase.
- Use this file as the quick lookup map before touching implementation.

## Source Scope
- Legacy root: `old-1.12.2`
- Main code root: `old-1.12.2/src/main/java/openblocks`
- Main assets root: `old-1.12.2/src/main/resources/assets/openblocks`

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
| flag | BlockFlag | `Blocks.flag` | TileEntityFlag | ItemFlagBlock | - |
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
| sky | BlockSky | `Blocks.sky` | TileEntitySky | ItemSkyBlock | unlocalized `sky.normal` |
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
| tasty_clay | ItemTastyClay | `Items.tastyClay` | `tastyClay` |
| golden_eye | ItemGoldenEye | `Items.goldenEye` | `goldenEye` |
| generic_unstackable | ItemOBGenericUnstackable | `Items.genericUnstackable` | no default model; `genericUnstackable` |
| cursor | ItemCursor | `Items.cursor` | - |
| info_book | ItemInfoBook | `Items.infoBook` | `infoBook` |
| dev_null | ItemDevNull | `Items.devNull` | `devnull` |
| sponge_on_a_stick | ItemSpongeOnAStick | `Items.spongeonastick` | `spongeonastick` |
| pedometer | ItemPedometer | `Items.pedometer` | - |
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
- `devNull` -> `ContainerDevNull` / `GuiDevNull`

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
  - `TriggerBrickDropped` -> trigger ID `openblocks:brick_dropped`
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
