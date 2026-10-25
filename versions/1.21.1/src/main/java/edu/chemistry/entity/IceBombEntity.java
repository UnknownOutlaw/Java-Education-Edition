package edu.chemistry.entity;

import edu.chemistry.registry.EduEntities;
import edu.chemistry.registry.EduItems;
import edu.chemistry.registry.EduSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.HitResult;

/** Freezes the water in a 3x3x3 area where it lands. Does no damage. */
public class IceBombEntity extends ThrowableItemProjectile {
    public IceBombEntity(EntityType<? extends IceBombEntity> type, Level level) {
        super(type, level);
    }

    public IceBombEntity(Level level, LivingEntity owner) {
        super(EduEntities.ICE_BOMB, owner, level);
    }

    public IceBombEntity(Level level, double x, double y, double z, ItemStack stack) {
        super(EduEntities.ICE_BOMB, x, y, z, level);
        this.setItem(stack);
    }

    @Override
    protected Item getDefaultItem() {
        return EduItems.ICE_BOMB;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            ItemStack item = this.getItem();
            for (int i = 0; i < 8; i++) {
                this.level().addParticle(new ItemParticleOption(ParticleTypes.ITEM, item.isEmpty() ? new ItemStack(EduItems.ICE_BOMB) : item),
                    this.getX(), this.getY(), this.getZ(), 0.0, 0.0, 0.0);
            }
            for (int i = 0; i < 12; i++) {
                this.level().addParticle(ParticleTypes.SNOWFLAKE, this.getX(), this.getY(), this.getZ(),
                    (this.random.nextDouble() - 0.5) * 0.3, this.random.nextDouble() * 0.2, (this.random.nextDouble() - 0.5) * 0.3);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        // landing in water counts as a hit, so it freezes the water around it (and anyone throwing it under water)
        if (!this.isRemoved() && this.level() instanceof ServerLevel level && this.isInWater()) {
            this.freeze(level, this.blockPosition());
        }
    }

    @Override
    protected void onHit(HitResult hitResult) {
        super.onHit(hitResult);
        if (this.level() instanceof ServerLevel level && !this.isRemoved()) {
            this.freeze(level, BlockPos.containing(hitResult.getLocation()));
        }
    }

    private void freeze(ServerLevel level, BlockPos center) {
        for (BlockPos p : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            BlockState state = level.getBlockState(p);
            if (state.getBlock() instanceof LiquidBlock && level.getFluidState(p).is(Fluids.WATER)) {
                level.setBlockAndUpdate(p, Blocks.ICE.defaultBlockState());
            }
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(), EduSounds.ICE_BOMB_HIT, SoundSource.NEUTRAL, 1.0F, 1.5F);
        level.broadcastEntityEvent(this, (byte) 3);
        this.discard();
    }
}
