# Other Minecraft versions

The Gradle project at the top of this repository builds the mod for **Minecraft 26.1.2**. These folders hold the same mod, version 2.2.0, for the other Minecraft versions it's released for:

| Folder | Minecraft | What's different |
| --- | --- | --- |
| `26.3` | 26.3 | The game's newer rendering and HUD code, data-driven brewing, and its own mixin targets |
| `1.21.1` | 1.21.1 | The older Fabric line: Mojang names that changed since (`ResourceLocation`, camelCase game rules...), older data formats, an access widener, and custom packets registered through the payload codec |

Each folder has the full `src/main/java` and `src/main/resources` for that version, laid out like the main project. They don't have a Gradle setup of their own:

- **26.3** is laid out for the same Gradle project: copy its `src` over the main one and set `minecraft_version` and `jei_version` in `gradle.properties` to 26.3's.
- **1.21.1** is written against Mojang's names and needs a Loom setup with Mojang mappings for 1.21.1. Its access widener (`edu.accesswidener`) is in Mojang's names too; Loom turns it into the game's own names when it builds the jar.

Neither of these has been built with Gradle; the released 26.3 and 1.21.1 jars are built from exactly this code.
