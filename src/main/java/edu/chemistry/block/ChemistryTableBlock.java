package edu.chemistry.block;

import edu.chemistry.menu.CompoundCreatorMenu;
import edu.chemistry.menu.ElementConstructorMenu;
import edu.chemistry.menu.LabTableMenu;
import edu.chemistry.menu.MaterialReducerMenu;
import edu.chemistry.registry.EduSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/** The four Education chemistry stations. They work like a crafting table: items go back to the player on close. */
public class ChemistryTableBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public enum Kind {
        ELEMENT_CONSTRUCTOR("container.edu.element_constructor"),
        COMPOUND_CREATOR("container.edu.compound_creator"),
        LAB_TABLE("container.edu.lab_table"),
        MATERIAL_REDUCER("container.edu.material_reducer");

        final String title;

        Kind(String title) {
            this.title = title;
        }
    }

    private final Kind kind;

    public ChemistryTableBlock(Kind kind, BlockBehaviour.Properties properties) {
        super(properties);
        this.kind = kind;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public Kind kind() {
        return this.kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            player.openMenu(state.getMenuProvider(level, pos));
            if (this.kind == Kind.ELEMENT_CONSTRUCTOR) {
                level.playSound(null, pos, EduSounds.ELEMENT_CONSTRUCTOR_OPEN, SoundSource.BLOCKS, 0.25F, 1.4F);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        ContainerLevelAccess access = ContainerLevelAccess.create(level, pos);
        Component title = Component.translatable(this.kind.title);
        return switch (this.kind) {
            case ELEMENT_CONSTRUCTOR -> new SimpleMenuProvider((id, inv, p) -> new ElementConstructorMenu(id, inv, access), title);
            case COMPOUND_CREATOR -> new SimpleMenuProvider((id, inv, p) -> new CompoundCreatorMenu(id, inv, access), title);
            case LAB_TABLE -> new SimpleMenuProvider((id, inv, p) -> new LabTableMenu(id, inv, access), title);
            case MATERIAL_REDUCER -> new SimpleMenuProvider((id, inv, p) -> new MaterialReducerMenu(id, inv, access), title);
        };
    }
}
