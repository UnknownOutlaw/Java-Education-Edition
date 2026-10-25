package edu.chemistry.block;

import edu.chemistry.EduRules;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Education Edition's chalkboards: the slate (1x1), poster (2x1) and board (3x2). They go on top of a block (standing on
 * legs) or on the side of one (flat against it), hold more text than a sign, can be edited again by using them, and
 * World Builders (creative players and operators) can lock them so nobody else can edit or break them.
 *
 * <p>Every cell of a chalkboard is one of these blocks; {@link #PART} says which cell (left to right, bottom to top, as
 * seen from the front). Cell 0 is the bottom-left one and holds the text in its {@link ChalkboardBlockEntity}.
 */
public class ChalkboardBlock extends Block implements EntityBlock {
    public enum Size {
        SLATE("slate", 1, 1, 6, 85),
        POSTER("poster", 2, 1, 6, 182),
        BOARD("board", 3, 2, 15, 279);

        public final String name;
        public final int width;
        public final int height;
        /** lines of text, like Bedrock (6, 6 and 15) */
        public final int lines;
        /**
         * how wide a line of text is, in font pixels (the editor box uses the same width): 42 lowercase i on a slate, or
         * "Tutorial, Product" (85) but not "Testing Area, and" (88), like Education,
         * and the same text size on the bigger boards (width in proportion to the slate inside the frame)
         */
        public final int textWidth;

        Size(String name, int width, int height, int lines, int textWidth) {
            this.name = name;
            this.width = width;
            this.height = height;
            this.lines = lines;
            this.textWidth = textWidth;
        }

        public int cells() {
            return this.width * this.height;
        }
    }

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** true = on the side of a block, false = standing on top of one */
    public static final BooleanProperty WALL = BooleanProperty.create("wall");
    /** standing slates turn to face you in 16 steps, like signs */
    public static final IntegerProperty ROTATION = BlockStateProperties.ROTATION_16;
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 5);

    /** set by the client so using a chalkboard opens the editor */
    public static Consumer<BlockPos> openEditor = pos -> {
    };

    public final Size size;

    public ChalkboardBlock(Size size, Properties properties) {
        super(properties);
        this.size = size;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WALL, false)
            .setValue(ROTATION, 0).setValue(PART, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WALL, ROTATION, PART);
    }

    /** Education's "World Builder" (given with /wb). Being in creative or an operator isn't enough, like Education. */
    public static boolean isWorldBuilder(Player player) {
        return edu.chemistry.EduPermissions.isWorldBuilder(player);
    }

    // ------------------------------------------------------------------ layout

    /** the direction to the viewer's right when looking at the front */
    public static Direction right(BlockState state) {
        return state.getValue(FACING).getCounterClockWise();
    }

    public static int col(BlockState state, Size size) {
        return state.getValue(PART) % size.width;
    }

    public static int row(BlockState state, Size size) {
        return state.getValue(PART) / size.width;
    }

    /** where cell {@code part} of the chalkboard whose bottom-left cell is {@code origin} goes */
    public static BlockPos cellPos(BlockPos origin, Direction right, Size size, int part) {
        return origin.relative(right, part % size.width).above(part / size.width);
    }

    public BlockPos originOf(BlockPos pos, BlockState state) {
        return pos.relative(right(state), -col(state, this.size)).below(row(state, this.size));
    }

    public static @Nullable ChalkboardBlockEntity entity(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChalkboardBlock board)) return null;
        return level.getBlockEntity(board.originOf(pos, state)) instanceof ChalkboardBlockEntity be ? be : null;
    }

    // ------------------------------------------------------------------ placing

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        BlockState state;
        if (face == Direction.UP) {
            state = this.defaultBlockState().setValue(WALL, false)
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(ROTATION, RotationSegment.convertToSegment(context.getRotation() + 180.0F));
        } else if (face.getAxis().isHorizontal()) {
            state = this.defaultBlockState().setValue(WALL, true).setValue(FACING, face);
        } else {
            return null;
        }
        // the whole chalkboard has to fit: it grows to the right and up from where you put it
        Level level = context.getLevel();
        BlockPos origin = context.getClickedPos();
        for (int part = 1; part < this.size.cells(); part++) {
            BlockPos p = cellPos(origin, right(state), this.size, part);
            if (!level.isInsideBuildHeight(p) || !level.getBlockState(p).canBeReplaced(context)) return null;
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        for (int part = 1; part < this.size.cells(); part++) {
            level.setBlock(cellPos(pos, right(state), this.size, part), state.setValue(PART, part), Block.UPDATE_ALL);
        }
        // one a World Builder puts up stays put: only World Builders can break it
        if (placer instanceof Player player && isWorldBuilder(player) && level.getBlockEntity(pos) instanceof ChalkboardBlockEntity be) {
            be.setBuilderPlaced(true);
        }
        // Bedrock opens the editor as soon as you place one
        if (level.isClientSide() && placer instanceof Player player && player.isLocalPlayer()) openEditor.accept(pos);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        // if a neighbouring cell of the same chalkboard is gone, the whole chalkboard goes
        BlockPos origin = this.originOf(pos, state);
        Direction right = right(state);
        for (int part = 0; part < this.size.cells(); part++) {
            if (!cellPos(origin, right, this.size, part).equals(neighborPos)) continue;
            boolean intact = neighbor.is(this) && neighbor.getValue(PART) == part && neighbor.getValue(FACING) == state.getValue(FACING)
                && neighbor.getValue(WALL) == state.getValue(WALL);
            return intact ? state : Blocks.AIR.defaultBlockState();
        }
        return state;
    }

    /**
     * A player breaking any cell takes the rest of the chalkboard with it without dropping anything, so only the broken
     * cell drops the chalkboard (and in creative, nothing drops at all). Without this the other cells would be knocked
     * out by the shape update below and each drop a copy.
     */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()) {
            BlockPos origin = this.originOf(pos, state);
            Direction right = right(state);
            for (int part = 0; part < this.size.cells(); part++) {
                BlockPos p = cellPos(origin, right, this.size, part);
                if (p.equals(pos) || !level.getBlockState(p).is(this)) continue;
                // UPDATE_KNOWN_SHAPE: don't let the broken cell see its neighbours vanish first, or it would vanish too
                // (dropping nothing) before the player's break could drop it
                level.setBlock(p, Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    // ------------------------------------------------------------------ using

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        ChalkboardBlockEntity be = entity(level, pos);
        if (be == null) return InteractionResult.PASS;
        if (level.isClientSide()) {
            if (!EduRules.clientEnabled) return InteractionResult.SUCCESS;
            if (be.isLocked() && !isWorldBuilder(player)) {
                player.sendOverlayMessage(Component.translatable("message.edu.chalkboard_locked").withStyle(ChatFormatting.RED));
            } else {
                openEditor.accept(be.getBlockPos());
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Locked chalkboards, and ones a World Builder placed, can only be broken by World Builders. */
    public static boolean protectedFrom(BlockGetter level, BlockPos pos, Player player) {
        ChalkboardBlockEntity be = entity(level, pos);
        return be != null && (be.isLocked() || be.isBuilderPlaced()) && !isWorldBuilder(player);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        if (protectedFrom(level, pos, player)) return 0.0F;
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? new ChalkboardBlockEntity(pos, state) : null;
    }

    // ------------------------------------------------------------------ looks

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(WALL)) {
            // a wall chalkboard stops 4px above the bottom of its lowest row
            double low = row(state, this.size) == 0 ? 4 : 0;
            return switch (state.getValue(FACING)) {
                case NORTH -> Block.box(0, low, 15, 16, 16, 16);
                case SOUTH -> Block.box(0, low, 0, 16, 16, 1);
                case WEST -> Block.box(15, low, 0, 16, 16, 16);
                default -> Block.box(0, low, 0, 1, 16, 16);
            };
        }
        if (this.size == Size.SLATE) return Block.box(3, 0, 3, 13, 16, 13);
        double bottom = row(state, this.size) == 0 ? 0 : 0;
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? Block.box(0, bottom, 7, 16, 16, 9) : Block.box(7, bottom, 0, 9, 16, 16);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)))
            .setValue(ROTATION, rotation.rotate(state.getValue(ROTATION), 16));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(ROTATION, mirror.mirror(state.getValue(ROTATION), 16));
    }
}
