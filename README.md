<p align="center">
  <img src="icon.png" alt="Education Edition" width="128">
</p>

<h1 align="center">Education Edition</h1>

<p align="center"><b>Minecraft Education's chemistry and classroom features, on Java Edition.</b></p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-26.1.2%20%7C%2026.3%20%7C%201.21.1-62B47A" alt="Minecraft 26.1.2 | 26.3 | 1.21.1">
  <img src="https://img.shields.io/badge/Version-2.2.1-8A6FD1" alt="Version 2.2.1">
  <img src="https://img.shields.io/badge/Loader-Fabric%20%7C%20NeoForge%20%7C%20Forge-DBD0B4" alt="Fabric | NeoForge | Forge">
  <img src="https://img.shields.io/badge/JEI-optional-4A90D9" alt="JEI optional">
  <img src="https://img.shields.io/badge/License-MIT-blue" alt="MIT License">
</p>

---

The chemistry and classroom content from Minecraft Education is some of the coolest stuff Minecraft has ever had, but it's been stuck on Bedrock, or behind a school login. This mod brings it over to Java on Fabric, NeoForge and Forge, kept as close to the original as possible.

Version **2.2.1** runs on **Minecraft 26.1.2, 26.3 and 1.21.1**, on **Fabric, NeoForge and Forge**: the same mod on every loader.

## Features

### The Agent
Your own robot helper, just like in Education. Place it with the **Agent Spawn Egg** (it faces away from you) or use `/agent create`. Every player gets their own Agent, and right-clicking it opens its 27-slot inventory.

- Control it with `/agent` commands: **move, turn, destroy, place, till, attack, collect, drop, dropall** and **transfer**. Ask it about the world with **detect, detectredstone, inspect, getitemcount, getitemdetail** and **getposition**.
- It presses pressure plates, sets off tripwires and travels through nether portals, end portals and end gateways. `/agent tp` brings it back from anywhere, even another dimension.
- It keeps the area around it loaded while its owner is online, so it keeps working even far away.

### Code Builder
Press **C** to open **MakeCode for Minecraft** in your web browser, connected to your world. Code with blocks, JavaScript or Python, press Run, and watch it happen in game.

- Programs can move and use the Agent, place and fill blocks, summon mobs, give items, teleport, and change the time, weather and game rules.
- They can react to chat commands, blocks broken or placed, mobs killed, arrows shot, pictures taken and more.
- The world's host and World Builders can use operator commands from Code Builder, even in a world without cheats, like in Education.
- Code Builder needs an internet connection. Your projects are saved in your browser.

### Chemistry
- **Element Constructor:** build any of the 118 elements by setting protons, neutrons and electrons. Change the neutrons to make isotopes.
- **Compound Creator:** combine elements into compounds. The recipe book shows every compound you've unlocked and can fill the grid for you.
- **Lab Table:** react elements and compounds together to make new items. Anything that isn't a real reaction fizzles out into Garbage.
- **Material Reducer:** break blocks and compounds back down into their elements.

The Element Constructor, Compound Creator and Lab Table can all be crafted. The Material Reducer can't, so get it from the Creative inventory or buy one from a Master Chemist.

### Products
Everything you make with chemistry: balloons, glow sticks, sparklers, colored torches, underwater torches, underwater TNT, ice bombs, super fertilizer, hardened glass, bleach and more.

- **Balloons** float, sway on their own, and tie to mobs, boats or fences with a real lead. Tie one to a mob and it lifts it off the ground. Hit the balloon and it drags the mob along. If it pops up high, whatever was tied to it falls back down.
- **Medicines** (Antidote, Elixir, Eye Drops and Tonic) are brewed in a regular brewing stand.

### Classroom tools
- **Chalkboards:** Slates, Posters and Boards to write on, on a wall or standing on legs. The text looks like it does in Education. World Builders can lock a board so only they can edit it, and boards a World Builder places can only be broken by World Builders.
- **Camera and Portfolio:** take pictures in game, or set the camera up on its tripod for a selfie. The pictures go into your portfolio, where you can caption them, export them all as a PDF, or turn them into photo items. A photo is held like a map and fills an item frame the way a map does.
- **NPCs:** place characters with names, dialogue and buttons that run commands. They turn to watch you when you get close, and they press pressure plates, set off tripwires and go through portals like mobs do.
- **Allow, Deny and Border blocks:** control where people can build and where they can go.

### Chemist villager
A villager without a job who finds a Lab Table becomes a **Chemist**. Chemists wear a lab coat and goggles and trade elements, salt, bleach, glow sticks, sparklers, balloons, medicines and more. Master Chemists sell the Material Reducer.

Villages can generate a **Chemist's lab**, with its own design for each village type: plains, desert, savanna, snowy and taiga. Each lab comes with a Lab Table, a loot chest of chemistry supplies and a villager to move in.

### Control tips
Education's on-screen control tips, using your own key bindings:
- **Right side:** what your buttons do right now (Place, Mine, Attack, Trade, Eat, Drop and so on). The list follows what you're looking at and holding, and moves up out of the way of closed captions.
- **Left side:** press **H** to open a panel of the movement and menu keys, including the Code Builder key, and number every hotbar slot with its key. Press **H** again to hide it.

Control tips can be switched off with the **Control Tips** toggle in **Options → Controls → Key Binds**. The H and C keys can be rebound there, under **Miscellaneous**.

### Chemistry Guide
Craft the guide book with a **book** and a **glass bottle**. It's an in-game book with every recipe in the mod, with pictures, and explains how the stations, classroom tools, the Agent and Code Builder work. Bookmarks on the side jump to each chapter.

## Turning it on
Everything in the mod is tied to a world setting, like Bedrock's Education Edition toggle. It's off by default.

- **New worlds:** switch on **Education Edition** in the **Game** tab of the world creation screen.
- **Existing worlds:** run `/gamerule edu:education_features true`.

While it's off, the Education Edition creative tab is hidden, labs don't generate in villages, and control tips, the Agent and Code Builder don't work.

## World Builder
| Command | What it does |
| --- | --- |
| `/wb` | Turns World Builder on or off for yourself |
| `/wb <players>` | Turns it on or off for those players |
| `/wb <players> true\|false` | Sets it for those players |

`/worldbuilder` works the same way. World Builders can build anywhere, edit NPCs and locked chalkboards, and ignore the permission blocks.

Only the world's host can use World Builder commands, or owner-level operators on a dedicated server. Players who join your world can't give it to themselves. The commands also work in command blocks, functions and the server console.

### Game rules
| Rule | Default | What it does |
| --- | --- | --- |
| `edu:education_features` | `false` | Switches all of the mod's content on or off |
| `edu:immutable_world` | `false` | Only World Builders can build, except above Allow blocks |

## Installation
1. Install a mod loader for your Minecraft version: [Fabric Loader](https://fabricmc.net/use/) with [Fabric API](https://modrinth.com/mod/fabric-api) in your `mods` folder, [NeoForge](https://neoforged.net/) or [Forge](https://files.minecraftforge.net/).
2. Download the jar for your Minecraft version and loader from the [releases](../../releases) and put it in your `mods` folder:

   | Minecraft | Fabric | NeoForge | Forge |
   | --- | --- | --- | --- |
   | 26.1.2 | `education-edition-<version>+26.1.2-fabric.jar` | `education-edition-<version>+26.1.2-neoforge.jar` | `education-edition-<version>+26.1.2-forge.jar` |
   | 26.3 | `education-edition-<version>+26.3-fabric.jar` | `education-edition-<version>+26.3-neoforge.jar` | `education-edition-<version>+26.3-forge.jar` |
   | 1.21.1 | `education-edition-<version>+1.21.1-fabric.jar` | `education-edition-<version>+1.21.1-neoforge.jar` | `education-edition-<version>+1.21.1-forge.jar` |

   Install only one Education Edition jar. Every jar has JEI support built in: with [JEI](https://modrinth.com/mod/jei) installed, it shows the element, compound and Lab Table recipes, and the medicines in JEI's brewing tab. You don't need JEI to play.

## Requirements
| | Minecraft 26.1.2 | Minecraft 26.3 | Minecraft 1.21.1 |
| --- | --- | --- | --- |
| Fabric | Fabric Loader 0.19.3 or newer, and Fabric API | Fabric Loader 0.19.3 or newer, and Fabric API | Fabric Loader 0.16.0 or newer, and Fabric API |
| NeoForge | 26.1.2 or newer | 26.3 or newer | 21.1 or newer |
| Forge | 64 or newer | 66 or newer | 52 or newer |
| Java | 25 | 25 | 21 |
| JEI | optional | optional | optional (a recent JEI 19 version) |

On 1.21.1, blocks and mobs added to Minecraft after that version can't be used from Code Builder, because they don't exist there.

## Building from source
You need **JDK 25** (Minecraft 26.1 runs on Java 25). Then, from the project folder:

```sh
./gradlew build        # Windows: gradlew.bat build
```

The jar ends up in `build/libs` as `education-edition-<version>+26.1.2-fabric.jar`. The mod's version number is set in `gradle.properties`.

The Gradle project builds the Fabric jar for 26.1.2. Every other jar is built from the same code: `src/main` is shared by all three loaders, and `src/fabric`, `src/neoforge` and `src/forge` add each loader's entry points and metadata. The 26.3 and 1.21.1 sources are in `versions/`, laid out the same way. There's no Gradle setup for NeoForge or Forge in this repository yet; see [versions/README.md](versions/README.md).

### Project layout
| Path | What's there |
| --- | --- |
| `src/main/java/edu/chemistry` | The mod's code, shared by every loader: blocks, items, entities, screens and mixins |
| `src/main/java/edu/chemistry/agent` | The Agent and its `/agent` commands |
| `src/main/java/edu/chemistry/code` | Code Builder in the world: MakeCode's messages, Bedrock commands and game events |
| `src/main/java/edu/chemistry/client/code` | Code Builder on your computer: the C key and the local server the MakeCode editor talks to |
| `src/main/java/edu/chemistry/network` | The mod's packets, handed to each loader's own networking |
| `src/main/java/edu/chemistry/compat/jei` | The JEI plugin (it only runs when JEI is installed) |
| `src/main/resources/assets/edu` | Textures, models, sounds, language and the guide book |
| `src/main/resources/data/edu` | Recipes, loot tables, tags, advancements, villager trades and the village labs |
| `src/fabric` | Fabric's entry points and its packet mixins |
| `src/neoforge` | NeoForge's entry points and `neoforge.mods.toml` |
| `src/forge` | Forge's entry points, `mods.toml` and `pack.mcmeta` |
| `versions/26.3` | The same mod for Minecraft 26.3 |
| `versions/1.21.1` | The same mod for Minecraft 1.21.1 |

## License
The mod's code is licensed under the [MIT License](LICENSE).

This is a fan port and isn't affiliated with Mojang or Microsoft. The original Minecraft textures, sounds and other assets belong to Mojang and aren't covered by the MIT license. Code Builder opens Microsoft's MakeCode for Minecraft website, which isn't part of the mod.

## Bugs and suggestions
Found a bug or something that doesn't match Education? Open an [issue](../../issues). The mod is still being worked on.
