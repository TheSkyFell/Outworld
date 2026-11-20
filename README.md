# Outworld

A Factorio-style space industry for Minecraft. Build a factory at home, launch satellites to survey
other worlds, fly there by shuttle, set up sealed habitats and outposts, and link everything back to
the main factory with drones and shuttles. Performance comes first: Outworld is built to stay light
on both servers and clients.

**Status:** early development (milestone M0, foundation). Nothing playable yet.

## Mods

Outworld is a suite of mods that release together with matching version numbers. Each feature mod
needs only Outworld Core, so you can install just the parts you want.

| Mod | Id | Status |
|---|---|---|
| Outworld Core | `outworld` | Started: the loader layer (registration, events, config, networking, menus and screens), the machine base with sleep and batching, energy, items and fluids that work with other mods' cables, pipes and machines, multiblocks defined in data files, performance measurement, `/outworld version`, `/outworld machines` and `/outworld perf` |
| Outworld Industry | `outworld_industry` | Started: ore processing (the crusher turns raw ore into two crushed ore; the ore washer turns two crushed ore and water into three purified ore; both smelt into ingots), the electric furnace (smelts anything a furnace does, twice as fast, on energy), the electrolyzer (splits water into hydrogen and oxygen), the gas liquefier (turns them into liquid hydrogen and liquid oxygen, the rocket propellants), the combustion generator (burns coal, charcoal, coal blocks, blaze rods or lava for 40 FE per tick), the solar panel (up to 16 FE per tick under open sky, less as the sun sets, nothing at night), cables (carry energy between generators, machines, batteries and other mods' storage), fluid and gas pipes and tanks (machines push the fluid and gas they make into them and draw what they need; tanks keep their contents; right-click a pipe or tank to see what it holds, or pour a bucket into a fluid tank), a creative tab, and temporary test blocks: a test machine (opens a screen that pings the server; a neighbour change gives it a 100-tick job that costs 10 FE per tick), a test battery (10,000 FE), a test crate (three slots; moves items downwards), a test tank (four buckets; moves fluid upwards) and a test multiblock controller (forms a 3x3x3 iron frame with an air core; right-click shows what's missing and marks where blocks go; right-click with iron blocks to place them) |
| Outworld Habitats | | Planned |
| Outworld Logistics | | Planned |
| Outworld Worlds | | Planned |

## Platform

- Minecraft 26.3, on NeoForge and Fabric (Fabric API required), from one codebase
- Energy is FE: NeoForge's energy API, and Team Reborn Energy on Fabric (bundled in Outworld Core)
- Items and fluids use each loader's own transfer API, so hoppers, pipes and buckets from any mod work with Outworld blocks. Fluids are counted in droplets (81,000 per bucket, as on Fabric); on NeoForge they move in whole millibuckets (81 droplets each)
- Java 25

## Building

```sh
./gradlew build
```

You need a JDK 25 installed, for example Temurin 25 from [adoptium.net](https://adoptium.net). Gradle
finds it on its own, even if another Java is your default. The mod jars end up in
`<module>/<loader>/build/libs/`, for example `industry/neoforge/build/libs/`. The first build takes
several minutes because it downloads and sets up Minecraft.

To start the game with Core and Industry loaded:

```sh
./gradlew :industry-neoforge:runClient   # or :industry-fabric:runClient
./gradlew :industry-neoforge:runServer   # or :industry-fabric:runServer
```

The `core-*` projects have the same run tasks and load Core on its own.

## Commands

| Command | Shows |
|---|---|
| `/outworld version` | The Outworld mods loaded, and their versions |
| `/outworld machines` | Machines loaded, awake and waiting (asleep, checking again in a moment) in each dimension, and how many multiblock positions are watched (needs the same permission as `/gamemode`) |
| `/outworld grid <pos>` | What the cable, pipe or tank grid at a position holds, how many blocks it has and how many neighbours it feeds (same permission) |
| `/outworld perf` | What Outworld costs the server over the last 100 ticks: the average and worst server tick, Outworld's share of it, and each system's time, with machines broken down by type (same permission) |

| `/outworld stress list \| start <scenario> [count] [x y z] \| status \| stop \| clear` | Builds a stress scenario and measures Outworld's cost under load and idle (needs the same permission as `/ban`; see below) |

Outworld's work also appears as named sections in Minecraft's own profiler (`/debug start`, `/jfr`).

## Stress testing

The v1 budget is under 5 ms of server time per tick for the whole mod, measured in a stress world
that grows with each milestone (500 machines, 5,000 belt sections, 500 drones, 10 habitats). Each
mod registers its scenarios; so far there is `machines`: test machines working nonstop, each
drawing energy from its own creative battery.

```
/outworld stress start machines 500 64 100 64
/outworld stress status
/outworld stress clear
```

A run keeps its area loaded, warms up for 5 seconds, measures 30 seconds under load, lets everything
finish and go to sleep, then measures 10 seconds idle. The result goes to the log, to
`/outworld stress status` and to `outworld-stress/<scenario>.json` in the game folder. CI runs the
`machines` scenario on both loaders, fails if it goes over budget or if anything stays awake once
it's quiet, and shows the numbers on each run's summary page.

## Project layout

Each mod is a folder with three Gradle projects:

```
core/
  common/     Shared code. Uses only vanilla Minecraft, never a loader API.
  fabric/     Fabric entry point and Fabric-specific glue.
  neoforge/   NeoForge entry point and NeoForge-specific glue.
industry/
  common/ fabric/ neoforge/
build-logic/  Gradle conventions shared by every project.
```

Each loader project compiles its mod's common code into its own jar, so every jar is a complete mod.
Common code reaches the loader only through Core, and feature mods talk to each other only through
Core.

Each mod's common code provides an `OutworldModule`, which lists what it registers, its network
payloads and its startup code. Mods with client-only parts (screens, handlers for payloads from the
server) also provide an `OutworldClientModule`. Each loader entry point is then one line: it hands the
module to Core, which registers everything at the moment that loader allows. See
`industry/fabric` and `industry/neoforge` for the whole of it.

Core gives common code:

| Need | Use |
|---|---|
| Register blocks, items, menus | `Registrar` (`core.registry`) |
| React to server start, ticks, commands | `OutworldEvents` (`core.event`); client: `OutworldClientEvents` |
| Settings | `ConfigFile` (`core.config`), a JSON file in `config/<mod id>/` |
| Network messages | `Payloads` in the module, `Network` to send; client: `ClientNetwork` |
| Menus that carry data to the client | `Menus` (`core.menu`); screens via `OutworldClientModule` |
| Machines | `MachineBlock` and `MachineBlockEntity` (`core.machine`): they work in batches every few ticks and sleep when idle, never ticking every tick |
| Recipe machines | `RecipeMachineBlock` and `RecipeMachineBlockEntity` (`core.machine`): item slots, fluid and gas tanks and energy, running recipes from `data/<namespace>/outworld/machine_recipe/` (see `MachineRecipe` for the format; loaded when a world loads). They push the fluid and gas they make into neighbouring storage, and when short of an input they draw it from neighbours. Override `findRecipe` to take recipes from elsewhere, as the electric furnace does with vanilla smelting. They share one screen (`MachineMenu`), which gets its energy, progress and tank readings only while it's open |
| Tank-only fluids | `OutworldFluid.create(key, color)` in a `Registrar` for `Registries.FLUID` makes a fluid that lives only in tanks and pipes (no block or bucket), named `fluid.<namespace>.<path>`; on NeoForge Core registers its fluid type |
| Gases | `GasTank` (`core.gas`), in millibuckets: gases are their own resource, separate from fluids. Gases are data (`data/<namespace>/outworld/gas/`; Core defines `outworld:hydrogen` and `outworld:oxygen`). Block entities expose tanks by implementing `GasHolder`; `GasTransfer.find` and `GasTransfer.move` reach them, and `GasTransfer.registerProvider` can bridge other mods' gas storage |
| Multiblocks | `MultiblockControllerBlockEntity` (`core.multiblock`): a machine that works only while its structure is complete. Structures are data files in `data/<namespace>/outworld/multiblock/` (see `MultiblockStructure` for the format), loaded when a world loads; a structure is re-checked only when one of its blocks changes. Controller blocks extend `MultiblockControllerBlock`: right-clicking one while holding a block the structure needs places it in the nearest empty spot; sneak-right-clicking (or right-clicking, unless it opens a screen) traces each missing block's spot in sparks (wrong blocks in orange) and says what's missing |
| Measuring cost | `Perf.section` (`core.perf`): time a system's work with `try (var _ = SECTION.start())`. Machines are timed automatically, per block entity type |
| Block entity types | `BlockEntityTypes` (`core.registry`) |
| Energy (FE) | `EnergyBuffer` (`core.energy`), exposed to other mods with `registerTransfers` in the module; `Energy.find` reaches any mod's energy storage, and `Energy.pushToNeighbours` / `pullFromNeighbours` move it to or from the six blocks around |
| Cables, pipes and tanks | Blocks implementing `EnergyConductor`, `FluidConductor` or `GasConductor` join every touching block of their kind into one grid with a single shared storage (`EnergyGrids.bufferAt`, `FluidGrids.tankAt`, `GasGrids.tankAt`); expose it to other mods with `blockEnergy` / `blockFluids`. A grid is worked out again only when one of its blocks or a block entity next to it is placed or broken, or a chunk loads or unloads, so a grid costs nothing per tick. Whatever is put into a grid is offered to the storage next to it at the end of that tick. Contents are saved with the world (`data/outworld/<kind>_grids.dat`). New kinds of grid: extend `GridKind` and pass it to `Grids.register` |
| Waiting on something that doesn't wake a machine | `retryIn(ticks)` in `MachineBlockEntity.work`: the machine sleeps but checks again after that many ticks (for example a machine short of energy, or a solar panel waiting for the sun) |
| Items | `ItemBuffer` (`core.item`): slots with input, output or storage roles, exposed with `registerTransfers`; `ItemTransfer.find` and `ItemTransfer.move` reach any mod's or vanilla's item storage |
| Fluids | `FluidTank` (`core.fluid`), in droplets (`FluidUnits`), exposed with `registerTransfers`; `FluidTransfer.find` and `FluidTransfer.move` reach any mod's fluid storage and vanilla cauldrons |

## License

[MIT](LICENSE)
