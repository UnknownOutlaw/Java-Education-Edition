# Other Minecraft versions and loaders

The Gradle project at the top of this repository builds the mod for **Minecraft 26.1.2** on **Fabric**. These folders hold the same mod, version 2.2.1, for the other Minecraft versions it's released for:

| Folder | Minecraft | What's different |
| --- | --- | --- |
| `26.3` | 26.3 | The game's newer rendering and HUD code, data-driven brewing, and its own mixin targets |
| `1.21.1` | 1.21.1 | The older line: Mojang names that changed since (`ResourceLocation`, camelCase game rules...), older data formats, an access widener, and a game-rule sync packet of its own |

Every Minecraft version, the main project included, is laid out the same way:

| Folder | What's there |
| --- | --- |
| `src/main` | The code and resources every loader shares |
| `src/fabric` | Fabric's entry points (`EduFabric`, `EduFabricClient`) and the mixins that carry the mod's packets on Fabric |
| `src/neoforge` | NeoForge's entry points (`EduNeoForge`, `EduNeoForgeClient`) and the `neoforge.mods.toml` the released jar carries |
| `src/forge` | Forge's entry points (`EduForge`, `EduForgeClient`), and the `mods.toml` and `pack.mcmeta` the released jar carries |

A jar for one loader is `src/main` plus that loader's folder. Some things each loader needs:

- **Fabric:** `fabric.mod.json` (in `src/main/resources`) lists both mixin configs, `edu.mixins.json` and `edu.fabric.mixins.json`.
- **NeoForge:** the mixins are listed in `neoforge.mods.toml`. The client's packet handler goes in through NeoForge's client event (26.x needs that).
- **Forge:** the jar's manifest needs a `MixinConfigs: edu.mixins.json` line, and Forge only reads the mod's resources when the jar has a `pack.mcmeta`.
- **1.21.1 on NeoForge and Forge:** `META-INF/accesstransformer.cfg` opens up the same things the Fabric access widener does.
- **JEI:** every jar has the JEI plugin (`edu.chemistry.compat.jei`), and JEI is an optional dependency everywhere. Nothing else in the mod touches JEI, so the plugin only runs when JEI is installed.

None of these folders has a Gradle setup of its own:

- **26.3 on Fabric** is laid out for the same Gradle project: copy its `src` over the main one and set `minecraft_version` and `jei_version` in `gradle.properties` to 26.3's.
- **1.21.1 on Fabric** is written against Mojang's names and needs a Loom setup with Mojang mappings for 1.21.1. Its access widener (`edu.accesswidener`) is in Mojang's names too; Loom turns it into the game's own names when it builds the jar.
- **NeoForge and Forge** need a ModDevGradle or ForgeGradle project that compiles `src/main` with `src/neoforge` or `src/forge`. NeoForge and Forge run the game with Mojang's names on all three versions, so the code needs no remapping.

The released jars are built from exactly this code.
