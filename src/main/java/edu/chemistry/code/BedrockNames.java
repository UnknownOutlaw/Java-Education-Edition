package edu.chemistry.code;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

/**
 * Bedrock block, item and mob names (as MakeCode sends them, with Bedrock's data values) to Java ids, and back. Most
 * names are the same in both games; the differences are older Bedrock names, data-value families, double slabs and
 * signs, and Education's own blocks and items (which this mod registers under Education's names).
 */
public final class BedrockNames {
    /** a Bedrock block or item as MakeCode names it */
    public record Ref(String id, int aux) {
    }

    /** Bedrock name -> Java ids to try in order (first that exists wins; version differences need more than one) */
    private static final Map<String, String[]> ALIASES = new HashMap<>();
    /** Bedrock name -> Java ids by data value */
    private static final Map<String, String[]> FAMILIES = new HashMap<>();
    /** Bedrock mob name -> Java ids to try */
    private static final Map<String, String[]> MOBS = new HashMap<>();
    /** Java id -> Bedrock name and data value (built from the tables above, plus a few extra) */
    private static final Map<String, Ref> REVERSE = new HashMap<>();
    private static final Map<String, String> MOBS_REVERSE = new HashMap<>();

    /** Bedrock's colour data values for wool, beds, stained glass... (white first) */
    static final String[] WOOL_COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
        "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"};
    /** Bedrock's colour data values for dyes and the dyed Education items (black first) */
    static final String[] DYE_COLORS = {"black", "red", "green", "brown", "blue", "purple", "cyan", "light_gray", "gray",
        "pink", "lime", "yellow", "light_blue", "magenta", "orange", "white"};
    /** Education's compound data values, in Bedrock's order */
    static final String[] COMPOUNDS = {"salt", "sodium_oxide", "lye", "magnesium_nitrate", "iron_sulfide", "lithium_hydride",
        "sodium_hydride", "calcium_bromide", "magnesium_oxide", "sodium_acetate", "luminol", "aluminum_oxide", "boron_trioxide",
        "soap", "polyethylene", "garbage", "magnesium_salts", "sulfate", "barium_sulfate", "potassium_chloride",
        "mercuric_chloride", "cerium_chloride", "tungsten_chloride", "calcium_chloride", "water", "glue", "hypochlorite",
        "crude_oil", "latex", "potassium_iodide", "sodium_fluoride", "benzene", "hydrogen_peroxide", "ammonia",
        "sodium_hypochlorite"};
    private static final String[] MEDICINES = {"edu:eye_drops", "edu:tonic", "edu:antidote", "edu:elixir"};
    private static final String[] SPARKLER_COLORS = {"orange", "blue", "red", "purple", "green"};

    private static void alias(String bedrock, String... java) {
        ALIASES.put(bedrock, java);
    }

    private static void family(String bedrock, String... java) {
        FAMILIES.put(bedrock, java);
    }

    private static void mob(String bedrock, String... java) {
        MOBS.put(bedrock, java);
    }

    static {
        alias("bed", "red_bed");
        alias("beetroot", "beetroot", "beetroots");
        alias("nether_brick", "nether_bricks");
        alias("brick_block", "bricks");
        alias("cave_vines_head_with_berries", "cave_vines[berries=true]");
        alias("cave_vines_body_with_berries", "cave_vines_plant[berries=true]");
        alias("chain", "iron_chain", "chain");
        alias("chemical_heat", "edu:heat_block");
        alias("daylight_detector_inverted", "daylight_detector[inverted=true]");
        alias("deadbush", "dead_bush");
        alias("dirt_with_roots", "rooted_dirt");
        alias("double_stone_slab", "smooth_stone_slab[type=double]");
        alias("double_stone_slab2", "red_sandstone_slab[type=double]");
        alias("double_wooden_slab", "oak_slab[type=double]");
        alias("normal_stone_double_slab", "stone_slab[type=double]");
        alias("end_brick_stairs", "end_stone_brick_stairs");
        alias("end_bricks", "end_stone_bricks");
        alias("fence_gate", "oak_fence_gate");
        alias("flowing_lava", "lava");
        alias("flowing_water", "water");
        alias("frame", "item_frame");
        alias("glow_frame", "glow_item_frame");
        alias("frog_spawn", "frogspawn");
        alias("golden_rail", "powered_rail");
        alias("grass_path", "dirt_path");
        alias("hardened_clay", "terracotta");
        alias("magma", "magma_block");
        alias("melon_block", "melon");
        alias("mob_spawner", "spawner");
        alias("noteblock", "note_block");
        alias("normal_stone_stairs", "stone_stairs");
        alias("piston_arm_collision", "piston_head");
        alias("pistonArmCollision", "piston_head");
        alias("sticky_piston_arm_collision", "piston_head[type=sticky]");
        alias("portal", "nether_portal");
        alias("powered_comparator", "comparator[powered=true]");
        alias("unpowered_comparator", "comparator");
        alias("powered_repeater", "repeater[powered=true]");
        alias("unpowered_repeater", "repeater");
        alias("prismarine_bricks_stairs", "prismarine_brick_stairs");
        alias("quartz_ore", "nether_quartz_ore");
        alias("red_nether_brick", "red_nether_bricks");
        alias("reeds", "sugar_cane");
        alias("silver_glazed_terracotta", "light_gray_glazed_terracotta");
        alias("silver_shulker_box", "light_gray_shulker_box");
        alias("undyed_shulker_box", "shulker_box");
        alias("skull", "skeleton_skull");
        alias("slime", "slime_block");
        alias("small_dripleaf_block", "small_dripleaf");
        alias("snow_layer", "snow");
        alias("standing_banner", "white_banner");
        alias("wall_banner", "white_wall_banner");
        alias("standing_sign", "oak_sign");
        alias("wall_sign", "oak_wall_sign");
        alias("darkoak_standing_sign", "dark_oak_sign");
        alias("darkoak_wall_sign", "dark_oak_wall_sign");
        alias("stonecutter_block", "stonecutter");
        alias("trapdoor", "oak_trapdoor");
        alias("trip_wire", "tripwire");
        alias("waterlily", "lily_pad");
        alias("waxed_copper", "waxed_copper_block");
        alias("web", "cobweb");
        alias("wooden_button", "oak_button");
        alias("wooden_door", "oak_door");
        alias("wooden_pressure_plate", "oak_pressure_plate");
        alias("yellow_flower", "dandelion");
        alias("azalea_leaves_flowered", "flowering_azalea_leaves");
        alias("unlit_redstone_torch", "redstone_torch[lit=false]");
        alias("invisibleBedrock", "barrier");
        alias("glowingobsidian", "obsidian");
        alias("movingBlock", "moving_piston");
        alias("lit_pumpkin", "jack_o_lantern");
        alias("lit_furnace", "furnace[lit=true]");
        alias("lit_blast_furnace", "blast_furnace[lit=true]");
        alias("lit_smoker", "smoker[lit=true]");
        alias("lit_redstone_lamp", "redstone_lamp[lit=true]");
        alias("lit_redstone_ore", "redstone_ore[lit=true]");
        alias("lit_deepslate_redstone_ore", "deepslate_redstone_ore[lit=true]");
        alias("grass", "grass_block");
        alias("double_plant", "sunflower");
        alias("red_flower", "poppy");
        alias("concretepowder", "white_concrete_powder");
        alias("stained_hardened_clay", "white_terracotta");
        // items
        alias("sign", "oak_sign");
        alias("empty_map", "map");
        alias("fireworks", "firework_rocket");
        alias("fireworksCharge", "firework_star");
        alias("netherbrick", "nether_brick");
        alias("clownfish", "tropical_fish");
        alias("zombie_pigman_spawn_egg", "zombified_piglin_spawn_egg");
        alias("rapid_fertilizer", "edu:super_fertilizer");
        alias("chalkboard", "edu:slate");
        alias("camera", "edu:camera");
        alias("record_11", "music_disc_11");
        alias("turtle_shell_piece", "turtle_scute");
        alias("scute", "turtle_scute");
        alias("lodestonecompass", "compass");
        alias("appleEnchanted", "enchanted_golden_apple");
        alias("melon", "melon_slice", "melon");
        alias("speckled_melon", "glistering_melon_slice");
        alias("muttonRaw", "mutton");
        alias("muttonCooked", "cooked_mutton");
        alias("dye", "black_dye");

        family("anvil", "anvil", "chipped_anvil", "damaged_anvil");
        family("cobblestone_wall", "cobblestone_wall", "mossy_cobblestone_wall", "granite_wall", "diorite_wall", "andesite_wall",
            "sandstone_wall", "brick_wall", "stone_brick_wall", "mossy_stone_brick_wall", "nether_brick_wall",
            "end_stone_brick_wall", "prismarine_wall", "red_sandstone_wall", "red_nether_brick_wall");
        family("dirt", "dirt", "coarse_dirt");
        family("prismarine", "prismarine", "dark_prismarine", "prismarine_bricks");
        family("purpur_block", "purpur_block", "purpur_block", "purpur_pillar", "purpur_block");
        family("quartz_block", "quartz_block", "chiseled_quartz_block", "quartz_pillar", "smooth_quartz");
        family("red_sandstone", "red_sandstone", "chiseled_red_sandstone", "cut_red_sandstone", "smooth_red_sandstone");
        family("sand", "sand", "red_sand");
        family("sandstone", "sandstone", "chiseled_sandstone", "cut_sandstone", "smooth_sandstone");
        family("sponge", "sponge", "wet_sponge");
        family("tnt", "tnt", "tnt", "edu:underwater_tnt", "edu:underwater_tnt");
        family("stonebrick", "stone_bricks", "mossy_stone_bricks", "cracked_stone_bricks", "chiseled_stone_bricks");
        family("monster_egg", "infested_stone", "infested_cobblestone", "infested_stone_bricks", "infested_mossy_stone_bricks",
            "infested_cracked_stone_bricks", "infested_chiseled_stone_bricks");
        family("stone_block_slab", "smooth_stone_slab", "sandstone_slab", "oak_slab", "cobblestone_slab", "brick_slab",
            "stone_brick_slab", "quartz_slab", "nether_brick_slab");
        family("stone_block_slab2", "red_sandstone_slab", "purpur_slab", "prismarine_slab", "dark_prismarine_slab",
            "prismarine_brick_slab", "mossy_cobblestone_slab", "smooth_sandstone_slab", "red_nether_brick_slab");
        family("stone_block_slab3", "end_stone_brick_slab", "smooth_red_sandstone_slab", "polished_andesite_slab", "andesite_slab",
            "diorite_slab", "polished_diorite_slab", "granite_slab", "polished_granite_slab");
        family("stone_block_slab4", "mossy_stone_brick_slab", "smooth_quartz_slab", "stone_slab", "cut_sandstone_slab",
            "cut_red_sandstone_slab");
        family("tallgrass", "short_grass", "short_grass", "fern", "fern");
        family("wood", "oak_wood", "spruce_wood", "birch_wood", "jungle_wood", "acacia_wood", "dark_oak_wood");
        family("board", "edu:slate", "edu:poster", "edu:board");
        family("stone", "stone", "granite", "polished_granite", "diorite", "polished_diorite", "andesite", "polished_andesite");
        family("planks", "oak_planks", "spruce_planks", "birch_planks", "jungle_planks", "acacia_planks", "dark_oak_planks");
        family("log", "oak_log", "spruce_log", "birch_log", "jungle_log");
        family("log2", "acacia_log", "dark_oak_log");
        family("leaves", "oak_leaves", "spruce_leaves", "birch_leaves", "jungle_leaves");
        family("leaves2", "acacia_leaves", "dark_oak_leaves");
        family("sapling", "oak_sapling", "spruce_sapling", "birch_sapling", "jungle_sapling", "acacia_sapling", "dark_oak_sapling");
        family("coral_fan_hang", "tube_coral_wall_fan", "brain_coral_wall_fan");
        family("coral_fan_hang2", "bubble_coral_wall_fan", "fire_coral_wall_fan");
        family("coral_fan_hang3", "horn_coral_wall_fan");
        family("medicine", MEDICINES);
        String[] wool = new String[16];
        String[] glass = new String[16];
        String[] pane = new String[16];
        String[] beds = new String[16];
        String[] banners = new String[16];
        String[] sticks = new String[16];
        String[] balloons = new String[16];
        String[] dyes = new String[16];
        for (int i = 0; i < 16; i++) {
            wool[i] = WOOL_COLORS[i] + "_wool";
            glass[i] = "edu:hard_" + WOOL_COLORS[i] + "_stained_glass";
            pane[i] = "edu:hard_" + WOOL_COLORS[i] + "_stained_glass_pane";
            beds[i] = WOOL_COLORS[i] + "_bed";
            banners[i] = DYE_COLORS[i] + "_banner";
            sticks[i] = "edu:glow_stick_" + DYE_COLORS[i];
            balloons[i] = "edu:balloon_" + DYE_COLORS[i];
            dyes[i] = DYE_COLORS[i] + "_dye";
        }
        family("wool", wool);
        family("hard_stained_glass", glass);
        family("hard_stained_glass_pane", pane);
        family("bed", beds);
        family("banner", banners);
        family("glow_stick", sticks);
        family("balloon", balloons);
        family("dye", dyes);
        String[] compounds = new String[COMPOUNDS.length];
        for (int i = 0; i < COMPOUNDS.length; i++) compounds[i] = "edu:" + COMPOUNDS[i];
        family("compound", compounds);

        mob("zombie_pigman", "zombified_piglin");
        mob("evocation_illager", "evoker");
        mob("evocation_fang", "evoker_fangs");
        mob("xp_bottle", "experience_bottle");
        mob("xp_orb", "experience_orb");
        mob("fireworks_rocket", "firework_rocket");
        mob("tropicalfish", "tropical_fish");
        mob("splash_potion", "splash_potion", "potion");
        mob("boat", "oak_boat", "boat");
        mob("chest_boat", "oak_chest_boat", "chest_boat");
        mob("ender_crystal", "end_crystal");
        mob("thrown_trident", "trident");
        mob("npc", "edu:npc");
        mob("agent", "edu:agent");
        mob("wither_skull_dangerous", "wither_skull");
        mob("lingering_potion", "lingering_potion", "potion");

        // Java -> Bedrock
        for (Map.Entry<String, String[]> e : FAMILIES.entrySet()) {
            String[] ids = e.getValue();
            // the anvil's data value is its damage times four (the low bits are which way it faces)
            int step = e.getKey().equals("anvil") ? 4 : 1;
            for (int i = ids.length - 1; i >= 0; i--) REVERSE.putIfAbsent(plain(ids[i]), new Ref(e.getKey(), i * step));
        }
        for (Map.Entry<String, String[]> e : ALIASES.entrySet()) {
            for (String id : e.getValue()) {
                if (id.indexOf('[') < 0) REVERSE.putIfAbsent(plain(id), new Ref(e.getKey(), 0));
            }
        }
        // flattened names MakeCode knows directly win over the families above
        for (String keep : new String[] {"stone", "oak_planks", "oak_log", "oak_leaves", "oak_sapling", "white_wool", "dirt",
            "sand", "sponge", "tnt", "sandstone", "red_sandstone", "prismarine", "anvil", "cobblestone_wall", "quartz_block",
            "purpur_block", "short_grass", "grass_block", "red_bed", "melon", "melon_slice", "map", "oak_sign", "red_flower"}) {
            REVERSE.remove(keep);
        }
        REVERSE.put("granite", new Ref("granite", 0));
        REVERSE.put("polished_granite", new Ref("polished_granite", 0));
        REVERSE.put("diorite", new Ref("diorite", 0));
        REVERSE.put("polished_diorite", new Ref("polished_diorite", 0));
        REVERSE.put("andesite", new Ref("andesite", 0));
        REVERSE.put("polished_andesite", new Ref("polished_andesite", 0));
        REVERSE.put("edu:heat_block", new Ref("chemical_heat", 0));
        REVERSE.put("edu:super_fertilizer", new Ref("rapid_fertilizer", 0));
        REVERSE.put("dark_oak_sign", new Ref("darkoak_standing_sign", 0));
        REVERSE.put("dark_oak_wall_sign", new Ref("darkoak_wall_sign", 0));
        REVERSE.put("oak_sign", new Ref("standing_sign", 0));
        REVERSE.put("cobweb", new Ref("web", 0));
        for (Map.Entry<String, String[]> e : MOBS.entrySet()) {
            for (String id : e.getValue()) MOBS_REVERSE.putIfAbsent(plain(id), e.getKey());
        }
        // before 1.21.5 Java has one thrown potion for both kinds: call it a splash potion
        MOBS_REVERSE.put("potion", "splash_potion");
    }

    /** the path of an id, without "minecraft:" (other namespaces are kept) and without block properties */
    private static String plain(String id) {
        int bracket = id.indexOf('[');
        if (bracket >= 0) id = id.substring(0, bracket);
        return id.startsWith("minecraft:") ? id.substring(10) : id;
    }

    private static Identifier idOf(String id) {
        int bracket = id.indexOf('[');
        return Identifier.parse(bracket >= 0 ? id.substring(0, bracket) : id);
    }

    private static boolean isBlock(String id) {
        try {
            return BuiltInRegistries.BLOCK.containsKey(idOf(id));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean isItem(String id) {
        try {
            return BuiltInRegistries.ITEM.containsKey(idOf(id));
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean isEntity(String id) {
        try {
            return BuiltInRegistries.ENTITY_TYPE.containsKey(idOf(id));
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Java ids a Bedrock name (and data value) could mean, best first */
    private static String[] candidates(String bedrock, int data) {
        String name = bedrock.startsWith("minecraft:") ? bedrock.substring(10) : bedrock;
        String[] family = FAMILIES.get(name);
        if (family != null) {
            int i = Math.max(0, data);
            if (name.equals("anvil")) i = i >> 2;
            else if (name.startsWith("stone_block_slab") || name.equals("wood") || name.equals("log") || name.equals("log2")) i = i & 7;
            else if (name.startsWith("coral_fan_hang")) i = i & 1;
            String pick = i < family.length ? family[i] : family[0];
            String[] rest = ALIASES.get(name);
            return rest == null ? new String[] {pick} : new String[] {pick, rest[0]};
        }
        String[] alias = ALIASES.get(name);
        if (alias != null) return alias;
        if (name.endsWith("_double_slab")) {
            String base = name.substring(0, name.length() - "_double_slab".length());
            if (base.endsWith("double_cut_copper")) base = base.replace("double_cut_copper", "cut_copper");
            return new String[] {base + "_slab[type=double]"};
        }
        if (name.startsWith("double_cut_copper_slab") || name.contains("_double_cut_copper_slab")) {
            return new String[] {name.replace("double_cut_copper_slab", "cut_copper_slab") + "[type=double]"};
        }
        if (name.endsWith("_standing_sign")) return new String[] {name.replace("darkoak", "dark_oak").replace("_standing_sign", "_sign")};
        if (name.startsWith("light_block_")) return new String[] {"light[level=" + name.substring(12) + "]"};
        if (name.startsWith("record_")) return new String[] {"music_disc_" + name.substring(7)};
        if (name.startsWith("sparkler")) {
            int i = Math.max(0, data);
            String color = i < DYE_COLORS.length ? DYE_COLORS[i] : "orange";
            for (String c : SPARKLER_COLORS) if (c.equals(color)) return new String[] {"edu:sparkler_" + c};
            return new String[] {"edu:sparkler_orange"};
        }
        if (name.equals("colored_torch_rg")) return new String[] {data >= 8 ? "edu:colored_torch_green" : "edu:colored_torch_red"};
        if (name.equals("colored_torch_bp")) return new String[] {data >= 8 ? "edu:colored_torch_purple" : "edu:colored_torch_blue"};
        if (name.equals("chemistry_table")) {
            String[] tables = {"edu:compound_creator", "edu:material_reducer", "edu:element_constructor", "edu:lab_table"};
            return new String[] {tables[Math.min(3, Math.max(0, data) >> 2)]};
        }
        if (name.equals("spawn_egg") && data > 0) {
            String mob = MakeCodeIds.mobName(data);
            String java = mob != null ? entity(mob) : null;
            if (java != null) return new String[] {plain(java) + "_spawn_egg"};
            if (mob != null) return new String[] {mob + "_spawn_egg", "edu:" + mob + "_spawn_egg"};
        }
        if (bedrock.indexOf(':') > 0) return new String[] {bedrock};
        return new String[] {name, "edu:" + name};
    }

    /** the Java block (an id, maybe with properties) for a Bedrock block name and data value, or null */
    public static @Nullable String block(String bedrock, int data) {
        for (String id : candidates(bedrock, data)) {
            if (isBlock(id)) return id;
        }
        return null;
    }

    /** the Java item id for a Bedrock item or block name and data value, or null */
    public static @Nullable String item(String bedrock, int data) {
        for (String id : candidates(bedrock, data)) {
            String plainId = id.indexOf('[') >= 0 ? id.substring(0, id.indexOf('[')) : id;
            if (isItem(plainId)) return plainId;
        }
        // blocks whose item is called something else (redstone wire -> redstone and so on)
        String block = block(bedrock, data);
        if (block != null) {
            Block b = BuiltInRegistries.BLOCK.getValue(idOf(block));
            Item item = b.asItem();
            if (item != net.minecraft.world.item.Items.AIR) return BuiltInRegistries.ITEM.getKey(item).toString();
        }
        return null;
    }

    /** the Java entity type id for a Bedrock mob name, or null */
    public static @Nullable String entity(String bedrock) {
        String name = bedrock.startsWith("minecraft:") ? bedrock.substring(10) : bedrock;
        String[] mapped = MOBS.get(name);
        for (String id : mapped != null ? mapped : new String[] {name, "edu:" + name}) {
            if (isEntity(id)) return id;
        }
        return null;
    }

    /** the Bedrock name and data value of a Java block, as MakeCode knows it */
    public static Ref bedrock(BlockState state) {
        Identifier key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String path = key.getNamespace().equals("minecraft") ? key.getPath() : key.toString();
        if (path.endsWith("_slab") && "double".equals(property(state, "type"))) {
            return new Ref(path.substring(0, path.length() - 5) + "_double_slab", 0);
        }
        if ("true".equals(property(state, "lit")) && (path.equals("furnace") || path.equals("blast_furnace") || path.equals("smoker")
            || path.equals("redstone_lamp") || path.equals("redstone_ore") || path.equals("deepslate_redstone_ore"))) {
            return new Ref("lit_" + path, 0);
        }
        return bedrockName(path);
    }

    private static @Nullable String property(BlockState state, String name) {
        for (Property<?> p : state.getProperties()) {
            if (p.getName().equals(name)) return valueName(state, p);
        }
        return null;
    }

    private static <T extends Comparable<T>> String valueName(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    /** the Bedrock name and data value of a Java item */
    public static Ref bedrock(Item item) {
        Identifier key = BuiltInRegistries.ITEM.getKey(item);
        return bedrockName(key.getNamespace().equals("minecraft") ? key.getPath() : key.toString());
    }

    /** a block or item path ("stone"), or a full id outside Minecraft's own ("edu:board") */
    private static Ref bedrockName(String path) {
        Ref ref = REVERSE.get(path);
        if (ref != null) return ref;
        // Education's own blocks and items have the same names here, without the namespace
        if (path.indexOf(':') >= 0) path = path.substring(path.indexOf(':') + 1);
        if (path.endsWith("_wall_sign") || path.endsWith("_sign") && !path.endsWith("hanging_sign")) {
            if (path.endsWith("_sign") && !path.endsWith("_wall_sign")) return new Ref(path.replace("_sign", "_standing_sign"), 0);
        }
        if (path.startsWith("music_disc_")) return new Ref("record_" + path.substring(11), 0);
        return new Ref(path, 0);
    }

    /** the Bedrock name of a Java mob */
    public static String bedrock(EntityType<?> type) {
        Identifier key = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        String path = key.getPath();
        String mapped = MOBS_REVERSE.get(key.getNamespace().equals("minecraft") ? path : key.toString());
        return mapped != null ? mapped : path;
    }

    private BedrockNames() {
    }
}
