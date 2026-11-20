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
| Outworld Core | `outworld` | Started: the loader layer (registration, events, config, networking, menus and screens), the machine base with sleep and batching, `/outworld version` and `/outworld machines` |
| Outworld Industry | `outworld_industry` | Started: one temporary test machine (opens a screen that pings the server; a neighbour change gives it a 100-tick job) |
| Outworld Habitats | | Planned |
| Outworld Logistics | | Planned |
| Outworld Worlds | | Planned |

## Platform

- Minecraft 26.3, on NeoForge and Fabric (Fabric API required), from one codebase
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
| Block entity types | `BlockEntityTypes` (`core.registry`) |

## License

[MIT](LICENSE)
