package edu.chemistry.entity;

import edu.chemistry.registry.EduBlocks;
import edu.chemistry.registry.EduEntities;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.FluidState;
import org.jspecify.annotations.Nullable;

/** Primed underwater TNT: water does not shield blocks from its explosion. */
public class UnderwaterPrimedTnt extends PrimedTnt {
    private static final ExplosionDamageCalculator IGNORE_WATER = new ExplosionDamageCalculator() {
        @Override
        public Optional<Float> getBlockExplosionResistance(Explosion explosion, BlockGetter level, BlockPos pos, BlockState block, FluidState fluid) {
            if (block.isAir()) {
                return Optional.empty();
            }
            if (block.getBlock() instanceof LiquidBlock) {
                return Optional.of(0.0F);
            }
            return Optional.of(block.getBlock().getExplosionResistance());
        }
    };

    public UnderwaterPrimedTnt(EntityType<? extends UnderwaterPrimedTnt> type, Level level) {
        super(type, level);
        this.setBlockState(EduBlocks.UNDERWATER_TNT.defaultBlockState());
    }

    public UnderwaterPrimedTnt(Level level, double x, double y, double z, @Nullable LivingEntity owner) {
        this(EduEntities.UNDERWATER_TNT, level);
        this.setPos(x, y, z);
        double rot = level.getRandom().nextDouble() * (Math.PI * 2);
        this.setDeltaMovement(-Math.sin(rot) * 0.02, 0.2F, -Math.cos(rot) * 0.02);
        this.setFuse(80);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    public static boolean tntExplodes(ServerLevel level) {
        return level.getGameRules().get(GameRules.TNT_EXPLODES);
    }

    @Override
    public void tick() {
        this.handlePortal();
        this.applyGravity();
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.applyEffectsFromBlocks();
        this.setDeltaMovement(this.getDeltaMovement().scale(0.98));
        if (this.onGround()) {
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.7, -0.5, 0.7));
        }
        int fuse = this.getFuse() - 1;
        this.setFuse(fuse);
        if (fuse <= 0) {
            this.discard();
            if (this.level() instanceof ServerLevel level && tntExplodes(level)) {
                level.explode(this, Explosion.getDefaultDamageSource(level, this), IGNORE_WATER, this.getX(), this.getY(0.0625), this.getZ(),
                    4.0F, false, Level.ExplosionInteraction.TNT);
            }
        } else {
            this.updateFluidInteraction();
            if (this.level().isClientSide()) {
                this.level().addParticle(this.isInWater() ? ParticleTypes.BUBBLE : ParticleTypes.SMOKE, this.getX(), this.getY() + 0.5, this.getZ(), 0.0, 0.0, 0.0);
            }
        }
    }
}
