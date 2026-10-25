package edu.chemistry.block;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/** Melts snow layers and ice within 2 blocks (taxicab distance) without giving off light. */
public class HeatBlock extends Block {
    private static final int RANGE = 2;
    private static final int INTERVAL = 20;

    public HeatBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this)) {
            level.scheduleTick(pos, this, INTERVAL);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        for (BlockPos p : BlockPos.withinManhattan(pos, RANGE, RANGE, RANGE)) {
            if (p.distManhattan(pos) > RANGE) continue;
            BlockState s = level.getBlockState(p);
            if (s.is(Blocks.SNOW)) {
                level.removeBlock(p, false);
            } else if (s.is(Blocks.ICE) || s.is(Blocks.FROSTED_ICE)) {
                if (level.dimensionType().ultraWarm()) {
                    level.removeBlock(p, false);
                } else {
                    level.setBlockAndUpdate(p, Blocks.WATER.defaultBlockState());
                    level.neighborChanged(p, Blocks.WATER, p);
                }
            }
        }
        level.scheduleTick(pos, this, INTERVAL);
    }
}
