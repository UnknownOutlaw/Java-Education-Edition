package edu.chemistry.registry;

import edu.chemistry.EduMod;
import edu.chemistry.data.ChemData;
import edu.chemistry.item.BalloonItem;
import edu.chemistry.item.CompoundItem;
import edu.chemistry.item.ElementBlockItem;
import edu.chemistry.item.GlowStickItem;
import edu.chemistry.item.IceBombItem;
import edu.chemistry.item.MedicineItem;
import edu.chemistry.item.SparklerItem;
import edu.chemistry.item.SuperFertilizerItem;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.level.block.Block;

public final class EduItems {
    public static final Item[] ELEMENTS = new Item[119];
    public static final Map<String, Item> COMPOUNDS = new LinkedHashMap<>();

    public static Item ELEMENT_CONSTRUCTOR;
    public static Item COMPOUND_CREATOR;
    public static Item LAB_TABLE;
    public static Item MATERIAL_REDUCER;
    public static Item HEAT_BLOCK;
    public static Item UNDERWATER_TNT;
    public static Item UNDERWATER_TORCH;
    public static Item HARD_GLASS;
    public static Item SLATE;
    public static Item NPC_SPAWN_EGG;
    public static Item AGENT_SPAWN_EGG;
    public static Item CAMERA;
    public static Item PORTFOLIO;
    public static Item PHOTO;
    public static Item ALLOW;
    public static Item DENY;
    public static Item BORDER;
    public static Item POSTER;
    public static Item BOARD;
    public static Item HARD_GLASS_PANE;
    public static final Map<DyeColor, Item> HARD_STAINED_GLASS = new EnumMap<>(DyeColor.class);
    public static final Map<DyeColor, Item> HARD_STAINED_GLASS_PANE = new EnumMap<>(DyeColor.class);
    public static final Map<String, Item> COLORED_TORCHES = new LinkedHashMap<>();

    public static Item BLEACH;
    public static Item ICE_BOMB;
    public static Item SUPER_FERTILIZER;
    public static Item ANTIDOTE;
    public static Item ELIXIR;
    public static Item EYE_DROPS;
    public static Item TONIC;
    public static Item GUIDE_BOOK;
    public static final Map<String, Item> SPARKLERS = new LinkedHashMap<>();
    public static final Map<DyeColor, Item> GLOW_STICKS = new EnumMap<>(DyeColor.class);
    public static final Map<DyeColor, Item> BALLOONS = new EnumMap<>(DyeColor.class);

    /** sparkler colour -> chloride that makes it */
    public static final List<String> SPARKLER_COLORS = List.of("orange", "blue", "red", "purple", "green");
    /** glow sticks exist in every dye colour except light gray and black */
    public static final List<DyeColor> GLOW_STICK_COLORS = List.of(DyeColor.WHITE, DyeColor.ORANGE, DyeColor.MAGENTA, DyeColor.LIGHT_BLUE,
        DyeColor.YELLOW, DyeColor.LIME, DyeColor.PINK, DyeColor.GRAY, DyeColor.CYAN, DyeColor.PURPLE, DyeColor.BLUE, DyeColor.BROWN,
        DyeColor.GREEN, DyeColor.RED);

    private static boolean done;

    public static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties props) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, EduMod.id(name));
        Item item = factory.apply(props.setId(key));
        if (item instanceof BlockItem blockItem) {
            blockItem.registerBlocks(Item.BY_BLOCK, item);
        }
        return Registry.register(BuiltInRegistries.ITEM, key, item);
    }

    public static Item register(String name, Function<Item.Properties, Item> factory) {
        return register(name, factory, new Item.Properties());
    }

    public static Item block(Block block) {
        return block(block, new Item.Properties());
    }

    public static Item block(Block block, Item.Properties props) {
        String name = BuiltInRegistries.BLOCK.getKey(block).getPath();
        return register(name, p -> new BlockItem(block, p), props.useBlockDescriptionPrefix());
    }

    public static void init() {
        if (done) return;
        done = true;
        for (int z = 0; z <= 118; z++) {
            final int atomicNumber = z;
            ELEMENTS[z] = register("element_" + z, p -> new ElementBlockItem(EduBlocks.ELEMENTS[atomicNumber], atomicNumber, p),
                new Item.Properties().useBlockDescriptionPrefix());
        }

        ELEMENT_CONSTRUCTOR = block(EduBlocks.ELEMENT_CONSTRUCTOR);
        COMPOUND_CREATOR = block(EduBlocks.COMPOUND_CREATOR);
        LAB_TABLE = block(EduBlocks.LAB_TABLE);
        MATERIAL_REDUCER = block(EduBlocks.MATERIAL_REDUCER);
        HEAT_BLOCK = block(EduBlocks.HEAT_BLOCK);
        UNDERWATER_TNT = block(EduBlocks.UNDERWATER_TNT);
        UNDERWATER_TORCH = register("underwater_torch",
            p -> new StandingAndWallBlockItem(EduBlocks.UNDERWATER_TORCH, EduBlocks.UNDERWATER_WALL_TORCH, Direction.DOWN, p),
            new Item.Properties().useBlockDescriptionPrefix());
        for (Map.Entry<String, Block[]> e : EduBlocks.COLORED_TORCHES.entrySet()) {
            Block[] torches = e.getValue();
            COLORED_TORCHES.put(e.getKey(), register("colored_torch_" + e.getKey(),
                p -> new StandingAndWallBlockItem(torches[0], torches[1], Direction.DOWN, p),
                new Item.Properties().useBlockDescriptionPrefix()));
        }
        HARD_GLASS = block(EduBlocks.HARD_GLASS);
        ALLOW = block(EduBlocks.ALLOW);
        DENY = block(EduBlocks.DENY);
        BORDER = block(EduBlocks.BORDER);
        CAMERA = register("camera", edu.chemistry.item.CameraItem::new);
        PORTFOLIO = register("portfolio", edu.chemistry.item.PortfolioItem::new);
        GUIDE_BOOK = register("guide_book", edu.chemistry.item.GuideBookItem::new, new Item.Properties().stacksTo(1));
        PHOTO = register("photo", edu.chemistry.item.PhotoItem::new, new Item.Properties().stacksTo(1));
        NPC_SPAWN_EGG = register("npc_spawn_egg", edu.chemistry.item.NpcSpawnEggItem::new);
        AGENT_SPAWN_EGG = register("agent_spawn_egg", edu.chemistry.item.AgentSpawnEggItem::new);
        SLATE = block(EduBlocks.SLATE, new Item.Properties().stacksTo(16));
        POSTER = block(EduBlocks.POSTER, new Item.Properties().stacksTo(16));
        BOARD = block(EduBlocks.BOARD, new Item.Properties().stacksTo(16));
        for (DyeColor c : DyeColor.values()) HARD_STAINED_GLASS.put(c, block(EduBlocks.HARD_STAINED_GLASS.get(c)));
        HARD_GLASS_PANE = block(EduBlocks.HARD_GLASS_PANE);
        for (DyeColor c : DyeColor.values()) HARD_STAINED_GLASS_PANE.put(c, block(EduBlocks.HARD_STAINED_GLASS_PANE.get(c)));

        for (int i = 0; i < ChemData.COMPOUND_IDS.length; i++) {
            final int index = i;
            String id = ChemData.COMPOUND_IDS[i];
            COMPOUNDS.put(id, register(id, p -> new CompoundItem(index, p)));
        }

        BLEACH = register("bleach", Item::new);
        ICE_BOMB = register("ice_bomb", IceBombItem::new, new Item.Properties().stacksTo(16));
        SUPER_FERTILIZER = register("super_fertilizer", SuperFertilizerItem::new);
        ANTIDOTE = register("antidote", p -> new MedicineItem(MobEffects.POISON, p), new Item.Properties().stacksTo(1));
        ELIXIR = register("elixir", p -> new MedicineItem(MobEffects.WEAKNESS, p), new Item.Properties().stacksTo(1));
        EYE_DROPS = register("eye_drops", p -> new MedicineItem(MobEffects.BLINDNESS, p), new Item.Properties().stacksTo(1));
        TONIC = register("tonic", p -> new MedicineItem(MobEffects.NAUSEA, p), new Item.Properties().stacksTo(1));

        for (String color : SPARKLER_COLORS) {
            SPARKLERS.put(color, register("sparkler_" + color, p -> new SparklerItem(color, p), new Item.Properties().durability(100)));
        }
        for (DyeColor color : GLOW_STICK_COLORS) {
            GLOW_STICKS.put(color, register("glow_stick_" + color.getSerializedName(), p -> new GlowStickItem(color, p),
                new Item.Properties().durability(100)));
        }
        for (DyeColor color : DyeColor.values()) {
            BALLOONS.put(color, register("balloon_" + color.getSerializedName(), p -> new BalloonItem(color, p), new Item.Properties()));
        }
    }

    public static Item compound(String id) {
        Item item = COMPOUNDS.get(id);
        if (item == null) throw new IllegalArgumentException("Unknown compound " + id);
        return item;
    }

    public static List<Item> elementList() {
        List<Item> list = new ArrayList<>();
        for (Item i : ELEMENTS) list.add(i);
        return list;
    }

    private EduItems() {
    }
}
