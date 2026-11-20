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
| Outworld Core | `outworld` | Started: loader layer and registration |
| Outworld Industry | `outworld_industry` | Started: one temporary test block |
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

Gradle needs Java 17 or newer installed to start. It downloads Java 25 for compiling if that's
missing. The mod jars end up in `<module>/<loader>/build/libs/`, for example
`industry/neoforge/build/libs/`. The first build takes several minutes because it downloads and sets
up Minecraft.

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
Common code reaches the loader only through Core's platform interfaces
(`io.github.theskyfell.outworld.core.platform`), and feature mods talk to each other only through
Core.

## License

[MIT](LICENSE)
