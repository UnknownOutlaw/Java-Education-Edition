package edu.chemistry.item;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/** Like bone meal, but a single use fully grows saplings and crops, and spreads flowers over a wider area. */
public class SuperFertilizerItem extends Item {
    public SuperFertilizerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof BonemealableBlock block) || !block.isValidBonemealTarget(level, pos, state)) {
            return InteractionResult.PASS;
        }
        if (state.getBlock() instanceof MangrovePropaguleBlock && state.getValue(MangrovePropaguleBlock.HANGING)) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        RandomSource random = server.getRandom();
        if (state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.MOSS_BLOCK)) {
            // spread over a wider area than bone meal
            for (BlockPos p : BlockPos.betweenClosed(pos.offset(-3, -1, -3), pos.offset(3, 1, 3))) {
                BlockState s = server.getBlockState(p);
                if (s.getBlock() instanceof BonemealableBlock b && s.is(state.getBlock()) && b.isValidBonemealTarget(server, p, s)
                    && (p.equals(pos) || random.nextInt(3) == 0)) {
                    b.performBonemeal(server, random, p.immutable(), s);
                    server.levelEvent(1505, p, 15);
                }
            }
        } else {
            // keep growing until it is fully grown (saplings turn into trees, crops reach max age);
            // anything else (flowers, mushrooms, ...) just gets a single bone meal effect
            BlockPos target = pos;
            boolean grows = state.getBlock() instanceof SaplingBlock || state.getBlock() instanceof CropBlock
                || state.getBlock() instanceof StemBlock || state.getBlock() instanceof CocoaBlock
                || state.getBlock() instanceof SweetBerryBushBlock || state.getBlock() instanceof MangrovePropaguleBlock;
            int tries = grows ? 64 : 1;
            for (int i = 0; i < tries; i++) {
                BlockState s = server.getBlockState(target);
                if (!(s.getBlock() instanceof BonemealableBlock b) || !b.isValidBonemealTarget(server, target, s)) break;
                b.performBonemeal(server, random, target, s);
            }
            server.levelEvent(1505, pos, 15);
        }
        ItemStack stack = context.getItemInHand();
        context.getLevel().gameEvent(context.getPlayer(), GameEvent.ITEM_INTERACT_FINISH, context.getClickedPos());
        stack.shrink(1);
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }
}
