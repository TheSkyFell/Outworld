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
| Outworld Industry | `outworld_industry` | Started: temporary test blocks: a test machine (opens a screen that pings the server; a neighbour change gives it a 100-tick job that costs 10 FE per tick), a test battery (10,000 FE), a test crate (three slots; moves items downwards), a test tank (four buckets; moves fluid upwards) and a test multiblock controller (forms a 3x3x3 iron frame with an air core; right-click shows what's missing and marks where blocks go) |
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
| `/outworld machines` | Machines loaded and awake in each dimension, and how many multiblock positions are watched (needs the same permission as `/gamemode`) |
| `/outworld perf` | What Outworld costs the server over the last 100 ticks: the average and worst server tick, Outworld's share of it, and each system's time, with machines broken down by type (same permission) |

Outworld's work also appears as named sections in Minecraft's own profiler (`/debug start`, `/jfr`).

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
| Multiblocks | `MultiblockControllerBlockEntity` (`core.multiblock`): a machine that works only while its structure is complete. Structures are data files in `data/<namespace>/outworld/multiblock/` (see `MultiblockStructure` for the format), loaded when a world loads; a structure is re-checked only when one of its blocks changes. Controller blocks extend `MultiblockControllerBlock`: sneak-right-clicking one (or right-clicking it, unless it opens a screen) traces each missing block's spot in sparks with the block floating inside (wrong blocks in orange) and says what's missing |
| Measuring cost | `Perf.section` (`core.perf`): time a system's work with `try (var _ = SECTION.start())`. Machines are timed automatically, per block entity type |
| Block entity types | `BlockEntityTypes` (`core.registry`) |
| Energy (FE) | `EnergyBuffer` (`core.energy`), exposed to other mods with `registerTransfers` in the module; `Energy.find` reaches any mod's energy storage |
| Items | `ItemBuffer` (`core.item`): slots with input, output or storage roles, exposed with `registerTransfers`; `ItemTransfer.find` and `ItemTransfer.move` reach any mod's or vanilla's item storage |
| Fluids | `FluidTank` (`core.fluid`), in droplets (`FluidUnits`), exposed with `registerTransfers`; `FluidTransfer.find` and `FluidTransfer.move` reach any mod's fluid storage and vanilla cauldrons |

## License

[MIT](LICENSE)
