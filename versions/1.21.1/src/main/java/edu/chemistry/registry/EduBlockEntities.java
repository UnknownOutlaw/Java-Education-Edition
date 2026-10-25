package edu.chemistry.registry;

import edu.chemistry.EduMod;
import edu.chemistry.block.ChalkboardBlockEntity;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class EduBlockEntities {
    public static BlockEntityType<ChalkboardBlockEntity> CHALKBOARD;

    @FunctionalInterface
    public interface Factory<T extends BlockEntity> {
        T create(BlockPos pos, BlockState state);
    }

    public static void init() {
        CHALKBOARD = register("chalkboard", ChalkboardBlockEntity::new, EduBlocks.SLATE, EduBlocks.POSTER, EduBlocks.BOARD);
    }

    /** BlockEntityType's supplier interface is private in vanilla; the mod's access widener opens it */
    private static <T extends BlockEntity> BlockEntityType<T> register(String name, Factory<T> factory, Block... blocks) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, EduMod.id(name),
            BlockEntityType.Builder.<T>of(factory::create, blocks).build(null));
    }

    private EduBlockEntities() {
    }
}
