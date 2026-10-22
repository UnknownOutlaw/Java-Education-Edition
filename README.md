<p align="center">
  <img src="icon.png" alt="Education Edition" width="128">
</p>

<h1 align="center">Education Edition</h1>

<p align="center"><b>Brings Education Edition to Java.</b></p>

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-26.1.2%20%7C%2026.3%20%7C%201.21.1-62B47A" alt="Minecraft 26.1.2 | 26.3 | 1.21.1">
  <img src="https://img.shields.io/badge/Version-2.0.1-8A6FD1" alt="Version 2.0.1">
  <img src="https://img.shields.io/badge/Loader-Fabric-DBD0B4" alt="Fabric">
  <img src="https://img.shields.io/badge/JEI-optional-4A90D9" alt="JEI optional">
  <img src="https://img.shields.io/badge/License-MIT-blue" alt="MIT License">
</p>

---

The chemistry and classroom content from Minecraft: Education Edition is some of the coolest stuff Minecraft has ever had, but it's been stuck on Bedrock, or behind a school login. This mod brings it over to Java on Fabric, kept as close to the original as possible, down to the creative menu order.

Version **2.0.1** runs on **Minecraft 26.1.2, 26.3 and 1.21.1**.

## Features

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
- **Chalkboards:** Slates, Posters and Boards to write on, on a wall or standing on legs. The text looks like it does in Education Edition. World Builders can lock a board so only they can edit it, and boards a World Builder places can only be broken by World Builders.
- **Camera and Portfolio:** take pictures in-game or set the camera up on its tripod for a selfie. The pictures go into your portfolio, where you can caption them, export them all as a PDF, or turn them into photo items. A photo is held like a map, showing its picture, and fills an item frame the way a map does.
- **NPCs:** place characters with names and dialogue around your world. They turn to watch you when you get close.
- **Allow, Deny and Border blocks:** control where people can build and where they can go.

### Chemist villager
A villager without a job who finds a Lab Table becomes a **Chemist**. Chemists wear a lab coat and goggles and trade elements, salt, bleach, glow sticks, sparklers, balloons, medicines and more. Master Chemists sell the Material Reducer.

Villages can generate a **Chemist's lab**, with its own design for each village type: plains, desert, savanna, snowy and taiga. Each lab comes with a Lab Table, a loot chest of chemistry supplies and a villager to move in.

### Control tips
Education Edition's on-screen control tips, using your own key bindings:
- **Right side:** what your buttons do right now (Place, Mine, Attack, Trade, Eat, Drop and so on). The list follows what you're looking at and holding, and moves up out of the way of closed captions.
- **Left side:** press **H** to open a panel of the movement and menu keys, and number every hotbar slot with its key. Press **H** again to hide it.

Control tips can be switched off with the **Control Tips** toggle in **Options → Controls → Key Binds** (with ReConsole: **Help & Options → Controls → Key Binds**). The H key can be rebound there, under **Miscellaneous**.

### Chemistry Guide
Craft the guide book with a **book** and a **glass bottle**. It's an in-game book with every recipe in the mod, with pictures, and explains how the stations and classroom tools work. Bookmarks on the side jump to each chapter.

## Turning it on
Everything in the mod is tied to a world setting, like Bedrock's Education Edition toggle. It's off by default.

- **New worlds:** switch on **Education Edition** in the **Game** tab of the world creation screen (with ReConsole: **More Options → Game Options**, under Host Privileges).
- **Existing worlds:** run `/gamerule edu:education_features true`.

While it's off, the Education Edition creative tab is hidden, labs don't generate in villages and control tips don't show.

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
1. Install [Fabric Loader](https://fabricmc.net/use/) for your Minecraft version.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) in your `mods` folder.
3. Download the jar for your Minecraft version and put it in your `mods` folder:
   - `education-edition-<version>+<minecraft>.jar` works on its own.
   - `education-edition-<version>+<minecraft>-jei.jar` also adds JEI support: element, compound and Lab Table recipes, and the medicines in JEI's brewing tab.

   `<minecraft>` is `26.1.2`, `26.3` or `1.21.1`. Install only one Education Edition jar. You don't need JEI to play.

## Requirements
| | Minecraft 26.1.2 / 26.3 | Minecraft 1.21.1 |
| --- | --- | --- |
| Fabric Loader | 0.19.3 or newer | 0.16.0 or newer |
| Fabric API | any | any for 1.21.1 |
| Java | 25 | 21 |
| JEI | optional | optional (a recent JEI 19 version) |
| ReConsole / Legacy4J | optional | optional |

## Building from source
You need **JDK 25** (Minecraft 26.1 runs on Java 25). Then, from the project folder:

```sh
./gradlew build        # Windows: gradlew.bat build
```

The jars end up in `build/libs`:
- `education-edition-<version>+26.1.2.jar` is the mod without the JEI plugin.
- `education-edition-<version>+26.1.2-jei.jar` is the same mod with the JEI plugin.

The mod's version number is set in `gradle.properties`.

### Project layout
| Path | What's there |
| --- | --- |
| `src/main/java/edu/chemistry` | The mod's code: blocks, items, entities, screens and mixins |
| `src/main/java/edu/chemistry/compat/jei` | The JEI plugin (left out of the plain jar) |
| `src/main/resources/assets/edu` | Textures, models, sounds, language and the guide book |
| `src/main/resources/data/edu` | Recipes, loot tables, tags, advancements, villager trades and the village labs |

## License
The mod's code is licensed under the [MIT License](LICENSE).

This is a fan port and isn't affiliated with Mojang or Microsoft. The original Minecraft textures, sounds and other assets belong to Mojang and aren't covered by the MIT license.

## Bugs and suggestions
Found a bug or something that doesn't match Education Edition? Open an [issue](../../issues). The mod is still being worked on.
