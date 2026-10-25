package edu.chemistry.registry;

import edu.chemistry.EduMod;
import edu.chemistry.block.ChemistryTableBlock;
import edu.chemistry.block.HeatBlock;
import edu.chemistry.block.UnderwaterTntBlock;
import edu.chemistry.block.UnderwaterTorchBlock;
import edu.chemistry.block.UnderwaterWallTorchBlock;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StainedGlassBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class EduBlocks {
    /** element_0 (unknown) .. element_118 (oganesson) */
    public static final Block[] ELEMENTS = new Block[119];

    public static Block ELEMENT_CONSTRUCTOR;
    public static Block COMPOUND_CREATOR;
    public static Block LAB_TABLE;
    public static Block MATERIAL_REDUCER;
    public static Block HEAT_BLOCK;
    public static Block UNDERWATER_TNT;
    public static Block UNDERWATER_TORCH;
    public static Block UNDERWATER_WALL_TORCH;

    public static Block HARD_GLASS;
    public static Block SLATE;
    public static Block ALLOW;
    public static Block DENY;
    public static Block BORDER;
    public static Block POSTER;
    public static Block BOARD;
    public static Block HARD_GLASS_PANE;
    public static final Map<DyeColor, Block> HARD_STAINED_GLASS = new EnumMap<>(DyeColor.class);
    public static final Map<DyeColor, Block> HARD_STAINED_GLASS_PANE = new EnumMap<>(DyeColor.class);

    /** colour name -> {standing, wall} */
    public static final Map<String, Block[]> COLORED_TORCHES = new LinkedHashMap<>();
    public static final List<String> TORCH_COLORS = List.of("blue", "red", "purple", "green");

    private static boolean done;

    public static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties props) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, EduMod.id(name));
        Block block = factory.apply(props.setId(key));
        return Registry.register(BuiltInRegistries.BLOCK, key, block);
    }

    private static BlockBehaviour.Properties wallVariant(Block standing) {
        return BlockBehaviour.Properties.of().overrideLootTable(standing.getLootTable()).overrideDescription(standing.getDescriptionId());
    }

    private static BlockBehaviour.Properties torchProps() {
        return BlockBehaviour.Properties.of().noCollision().instabreak().lightLevel(s -> 14).sound(SoundType.WOOD).pushReaction(PushReaction.POPPED);
    }

    private static BlockBehaviour.Properties permissionProps(MapColor color, NoteBlockInstrument instrument) {
        return BlockBehaviour.Properties.of().mapColor(color).instrument(instrument).strength(-1.0F, 3600000.0F).noLootTable()
            .sound(SoundType.STONE).pushReaction(PushReaction.IMMOVEABLE).isValidSpawn((s, l, p, e) -> false);
    }

    private static BlockBehaviour.Properties hardGlassProps(MapColor color) {
        return BlockBehaviour.Properties.of()
            .mapColor(color)
            .instrument(NoteBlockInstrument.HAT)
            // same blast resistance as obsidian (1200)
            .strength(10.0F, 1200.0F)
            .sound(SoundType.GLASS)
            .noOcclusion()
            .isValidSpawn((s, l, p, e) -> false)
            .isRedstoneConductor((s, l, p) -> false)
            .isSuffocating((s, l, p) -> false)
            .isViewBlocking((s, l, p, box) -> false);
    }

    public static void init() {
        if (done) return;
        done = true;
        for (int z = 0; z <= 118; z++) {
            ELEMENTS[z] = register("element_" + z, Block::new,
                BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(1.0F, 6.0F).sound(SoundType.STONE));
        }

        BlockBehaviour.Properties machine = BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5F).sound(SoundType.METAL);
        ELEMENT_CONSTRUCTOR = register("element_constructor", p -> new ChemistryTableBlock(ChemistryTableBlock.Kind.ELEMENT_CONSTRUCTOR, p), machine);
        COMPOUND_CREATOR = register("compound_creator", p -> new ChemistryTableBlock(ChemistryTableBlock.Kind.COMPOUND_CREATOR, p),
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5F).sound(SoundType.METAL).noOcclusion());
        LAB_TABLE = register("lab_table", p -> new ChemistryTableBlock(ChemistryTableBlock.Kind.LAB_TABLE, p),
            BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5F).sound(SoundType.METAL));
        MATERIAL_REDUCER = register("material_reducer", p -> new ChemistryTableBlock(ChemistryTableBlock.Kind.MATERIAL_REDUCER, p),
            BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(2.5F).sound(SoundType.METAL));

        HEAT_BLOCK = register("heat_block", HeatBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(2.5F).requiresCorrectToolForDrops().sound(SoundType.METAL).noOcclusion());
        UNDERWATER_TNT = register("underwater_tnt", UnderwaterTntBlock::new,
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).instabreak().sound(SoundType.GRASS).ignitedByLava().isRedstoneConductor((s, l, p) -> false));

        UNDERWATER_TORCH = register("underwater_torch", UnderwaterTorchBlock::new, torchProps());
        UNDERWATER_WALL_TORCH = register("underwater_wall_torch", UnderwaterWallTorchBlock::new,
            wallVariant(UNDERWATER_TORCH).noCollision().instabreak().lightLevel(s -> 14).sound(SoundType.WOOD).pushReaction(PushReaction.POPPED));

        for (String color : TORCH_COLORS) {
            SimpleParticleType flame = ParticleTypes.FLAME;
            Block standing = register("colored_torch_" + color, p -> new TorchBlock(flame, p) {
            }, torchProps());
            Block wall = register("colored_wall_torch_" + color, p -> new WallTorchBlock(flame, p) {
            }, wallVariant(standing).noCollision().instabreak().lightLevel(s -> 14).sound(SoundType.WOOD).pushReaction(PushReaction.POPPED));
            COLORED_TORCHES.put(color, new Block[] {standing, wall});
        }

        for (edu.chemistry.block.ChalkboardBlock.Size size : edu.chemistry.block.ChalkboardBlock.Size.values()) {
            Block b = register(size.name, p -> new edu.chemistry.block.ChalkboardBlock(size, p), BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_BLACK).instrument(NoteBlockInstrument.BASS).strength(1.15F, 1.0F).sound(SoundType.STONE)
                .noCollision().noOcclusion().pushReaction(PushReaction.IMMOVEABLE));
            switch (size) {
                case SLATE -> SLATE = b;
                case POSTER -> POSTER = b;
                case BOARD -> BOARD = b;
            }
        }

        // Education's permission blocks: unbreakable except by World Builders, can't be pushed, drop nothing
        ALLOW = register("allow", Block::new, permissionProps(MapColor.COLOR_YELLOW, NoteBlockInstrument.HARP));
        DENY = register("deny", Block::new, permissionProps(MapColor.COLOR_GRAY, NoteBlockInstrument.HARP));
        BORDER = register("border_block", p -> new net.minecraft.world.level.block.WallBlock(p) {
        }, permissionProps(MapColor.COLOR_RED, NoteBlockInstrument.BASEDRUM).noOcclusion());

        HARD_GLASS = register("hard_glass", p -> new TransparentBlock(p) {
        }, hardGlassProps(MapColor.NONE));
        HARD_GLASS_PANE = register("hard_glass_pane", p -> new IronBarsBlock(p) {
        }, hardGlassProps(MapColor.NONE));
        for (DyeColor color : DyeColor.values()) {
            HARD_STAINED_GLASS.put(color, register("hard_" + color.getSerializedName() + "_stained_glass",
                p -> new StainedGlassBlock(color, p), hardGlassProps(color.getMapColor())));
        }
        for (DyeColor color : DyeColor.values()) {
            HARD_STAINED_GLASS_PANE.put(color, register("hard_" + color.getSerializedName() + "_stained_glass_pane",
                p -> new StainedGlassPaneBlock(color, p), hardGlassProps(color.getMapColor())));
        }
    }

    public static List<Block> all() {
        List<Block> list = new ArrayList<>();
        for (Block b : ELEMENTS) list.add(b);
        return list;
    }

    private EduBlocks() {
    }
}
