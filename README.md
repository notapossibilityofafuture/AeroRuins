# AeroRuins

A native Kotlin world-generation plugin for Paper 1.21.11 that creates a large
liminal survival world inspired by Frutiger Aero, abandoned cities, underground
ruins and The Veil.

Developed by **notapossibilityofafuture**.

## Features

| System | Description |
|---|---|
| Custom World Gen | Native terrain generation with custom climates, rivers, lakes, mountains and terrain variation. |
| Liminal Zones | Backrooms, malls, quarries, flooded subway areas and abandoned urban spaces. |
| The Veil | Three extra dimensions: Dermis, Hypodermis and Muscle, generated from the same world seed. |
| Cave Field | Large caves, tunnels, canyons, underground lakes, lava, geodes and themed cave biomes. |
| Ruins and Cities | Abandoned towns, villages, roads, towers, apartments, suburbs, domes and industrial ruins. |
| Biome Datapack | 106 custom biomes with custom colors, fog, particles, mob spawns and cave themes. |
| Loot | Custom loot tables with equipment, enchanted books, rare items and themed supplies. |
| Portals | Tears and portal platforms connect the surface with The Veil dimensions. |
| Protection and Balance | Configurable mob rates, fauna density, chest rarity and structure spawning. |
| World Border | Optional automatic world border configured per server. |
| Automatic Datapack | Optional HTTPS download and installation before a new AeroRuins world is created. |
| Worldgen Editor | Optional Tkinter editor for configuration, JSON files, regeneration and builds. |

## Requirements

- Paper 1.21.11
- Java 21

The plugin still works without the custom datapack, but custom biome colors,
particles, mob spawns and datapack loot require it.

## Installation

1. Download the latest `AeroRuins.jar`.
2. Put it in the server `plugins` folder.
3. Start the server once to create `plugins/AeroRuins/config.yml`.
4. Configure the plugin if needed.
5. Restart the server.
6. Create a new world with `/aeroworld <name>`.

World generation changes only affect new chunks. For reliable results, create a
new world after replacing or regenerating the datapack.

## Commands

```text
/aeroworld [name]
/aerotp <town|city|village|tear|dome|garden|flowers|neighborhood|stronghold|...>
/aeroveil <surface|dermis|hypodermis|muscle>
```

`/aeroworld` creates or loads an AeroRuins world and, when enabled, its three
The Veil dimensions.

## Automatic Datapack Download

Minecraft datapacks cannot download files by themselves. AeroRuins can
optionally download the datapack through the plugin before creating a world.

Set these values in `plugins/AeroRuins/config.yml`:

```yaml
datapack-auto-download: true
datapack-url: "https://example.com/aeroruins-datapack.zip"
datapack-file-name: aeroruins-datapack.zip
datapack-timeout-seconds: 30
datapack-max-size-mb: 64
```

The URL must use HTTPS and point directly to a ZIP file. GitHub Release assets
are recommended. The plugin validates `pack.mcmeta`, limits the download size,
keeps a local cache and installs the ZIP into the world's `datapacks` folder
before world creation.

## Configuration

Important settings include:

```yaml
caves: true
chests: true
chest-rarity: 1.0
mobs: true
classic-structures: true
liminal-chance: 45
world-size: 20000
dome-spacing: 7000
road-spacing: 1024
mob-spawn-rate: 0.25
structure-mob-rate: 0.4
fauna-density: 1.0
veil-dimensions: true
```

See `src/main/resources/config.yml` for every available option.

## Worldgen Editor

The project includes a dependency-free Python editor based on Tkinter:

```powershell
python tools\worldgen_editor.py
```

The editor can:

- edit every `config.yml` world-generation setting;
- edit and validate datapack JSON files;
- create automatic backups;
- regenerate the biome datapack;
- regenerate loot tables;
- build the plugin.

The generator scripts can also be executed directly:

```powershell
python tools\gen_world.py <datapack-folder> <Climate.kt-output>
python tools\gen_loot.py <datapack-folder>
```

## Build From Source

Requirements: Java 21 and the included Gradle wrapper.

```powershell
.\gradlew.bat build
```

The compiled plugin is created at:

```text
build\libs\AeroRuins-2.2.0.jar
```

## Architecture

The plugin is organized into focused Kotlin systems:

```text
AeroRuins
├── World Generator
├── Biome Provider
├── Cave Field
├── Climate and Terrain
├── Cities and Roads
├── Ruins and Structures
├── The Veil Dimensions
├── Portals
├── Loot
└── Configuration
```

The custom biome and loot datapack is generated from the Python tools so that
the Kotlin biome registry, biome JSON and loot tables stay synchronized.

## Support and Contact

For support, licensing, custom branding or development requests:

- Discord: `notapossibilityofafuture`
- Email: `napoaf@icloud.com`

## License

Copyright belongs to the developer. Contact the developer before redistributing,
modifying or licensing this plugin.
