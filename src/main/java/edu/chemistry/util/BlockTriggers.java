package edu.chemistry.util;

import edu.chemistry.agent.AgentEntity;
import edu.chemistry.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.TripWireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Agent and NPCs press pressure plates, set off tripwires and go through portals (nether portals, end portals and
 * end gateways), like mobs do. Neither one moves the way mobs do, so the game never tells those blocks they're there:
 * they do that themselves each tick, for those blocks only (not fire, lava, cobwebs and the rest). Stone and blackstone
 * plates only notice living mobs; PressurePlateBlockMixin makes them notice these two as well.
 */
public final class BlockTriggers {
    /** tells the pressure plates, tripwires and portals this entity is touching that it's there */
    public static void touch(Entity entity) {
        Level level = entity.level();
        if (level.isClientSide() || entity.isRemoved()) return;
        AABB box = entity.getBoundingBox().deflate(1.0E-5);
        int x0 = Mth.floor(box.minX);
        int x1 = Mth.floor(box.maxX);
        int y0 = Mth.floor(box.minY);
        int y1 = Mth.floor(box.maxY);
        int z0 = Mth.floor(box.minZ);
        int z1 = Mth.floor(box.maxZ);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    Block block = state.getBlock();
                    if (!(block instanceof BasePressurePlateBlock || block instanceof TripWireBlock || block instanceof Portal)) continue;
                    // only when it's really in the block's shape (a tripwire string, the plate, the portal's middle)
                    VoxelShape shape = state.getShape(level, pos);
                    if (shape.isEmpty() || !shape.bounds().move(pos).intersects(box)) continue;
                    state.entityInside(level, pos.immutable(), entity, InsideBlockEffectApplier.NOOP, true);
                    if (entity.isRemoved()) return;
                }
            }
        }
    }

    /** whether an Agent or NPC is on the pressure plate at this position (the plate's own touch area) */
    public static boolean agentOrNpcOn(Level level, BlockPos pos) {
        AABB plate = new AABB(pos.getX() + 0.0625, pos.getY(), pos.getZ() + 0.0625, pos.getX() + 0.9375, pos.getY() + 0.25,
            pos.getZ() + 0.9375);
        return !level.getEntitiesOfClass(Entity.class, plate,
            e -> (e instanceof AgentEntity || e instanceof NpcEntity) && e.isAlive() && !e.isIgnoringBlockTriggers()).isEmpty();
    }

    private BlockTriggers() {
    }
}
