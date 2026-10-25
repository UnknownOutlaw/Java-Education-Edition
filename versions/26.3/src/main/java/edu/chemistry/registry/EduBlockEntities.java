package edu.chemistry.registry;

import edu.chemistry.EduMod;
import edu.chemistry.block.ChalkboardBlockEntity;
import edu.chemistry.util.Reflect;
import java.lang.reflect.Constructor;
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

    /** BlockEntityType's constructor and its supplier interface are private in vanilla (Fabric API would normally expose them). */
    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> BlockEntityType<T> register(String name, Factory<T> factory, Block... blocks) {
        try {
            Class<?> supplier = Reflect.cls(BlockEntityType.class, "net.minecraft.world.level.block.entity.BlockEntityType$BlockEntitySupplier");
            Object proxy = Reflect.implement(supplier, "create", args -> factory.create((BlockPos) args[0], (BlockState) args[1]));
            Constructor<?> ctor = BlockEntityType.class.getDeclaredConstructor(supplier, Set.class);
            ctor.setAccessible(true);
            BlockEntityType<T> type = (BlockEntityType<T>) ctor.newInstance(proxy, Set.of(blocks));
            return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, EduMod.id(name), type);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Couldn't register block entity " + name, e);
        }
    }

    private EduBlockEntities() {
    }
}
