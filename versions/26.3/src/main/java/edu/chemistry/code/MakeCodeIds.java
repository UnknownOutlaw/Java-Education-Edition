package edu.chemistry.code;

import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/** MakeCode's numeric mob ids (Bedrock's), used by spawn eggs with a data value and by "mob spawned" events. */
public final class MakeCodeIds {
    private static final Map<Integer, String> MOBS = new HashMap<>();
    private static final Map<String, Integer> MOB_IDS = new HashMap<>();

    static {
        String table = "10 chicken 11 cow 12 pig 13 sheep 14 wolf 15 villager 16 mooshroom 17 squid 18 rabbit 19 bat 20 iron_golem "
            + "21 snow_golem 22 ocelot 23 horse 24 donkey 25 mule 26 skeleton_horse 27 zombie_horse 28 polar_bear 29 llama "
            + "30 parrot 31 dolphin 32 zombie 33 creeper 34 skeleton 35 spider 36 zombie_pigman 37 slime 38 enderman "
            + "39 silverfish 40 cave_spider 41 ghast 42 magma_cube 43 blaze 44 zombie_villager 45 witch 46 stray 47 husk "
            + "48 wither_skeleton 49 guardian 50 elder_guardian 51 npc 52 wither 53 ender_dragon 54 shulker 55 endermite "
            + "56 agent 57 vindicator 58 phantom 59 ravager 65 tnt 68 xp_bottle 69 xp_orb 72 fireworks_rocket 74 turtle 75 cat "
            + "80 arrow 81 snowball 82 egg 84 minecart 86 splash_potion 90 boat 93 lightning_bolt 96 hopper_minecart "
            + "97 tnt_minecart 98 chest_minecart 100 command_block_minecart 103 evocation_fang 104 evocation_illager 105 vex "
            + "108 pufferfish 109 salmon 110 drowned 111 tropicalfish 112 cod 113 panda 114 pillager 118 wandering_trader "
            + "121 fox 122 bee 123 axolotl 124 glow_squid 125 goat 126 hoglin 127 piglin 128 strider 129 zoglin 130 allay "
            + "131 frog 132 tadpole 133 warden 134 camel 135 sniffer 136 armadillo 137 breeze 139 creaking 141 happy_ghast "
            + "143 piglin_brute";
        String[] parts = table.split(" ");
        for (int i = 0; i + 1 < parts.length; i += 2) {
            int id = Integer.parseInt(parts[i]);
            MOBS.put(id, parts[i + 1]);
            MOB_IDS.putIfAbsent(parts[i + 1], id);
        }
    }

    public static @Nullable String mobName(int id) {
        return MOBS.get(id);
    }

    /** MakeCode's number for a Bedrock mob name, or -1 */
    public static int mobId(String bedrockName) {
        Integer id = MOB_IDS.get(bedrockName);
        return id != null ? id : -1;
    }

    private MakeCodeIds() {
    }
}
