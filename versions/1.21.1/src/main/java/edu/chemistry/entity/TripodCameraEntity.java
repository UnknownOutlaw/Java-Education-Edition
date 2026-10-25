package edu.chemistry.entity;

import net.minecraft.nbt.CompoundTag;
import edu.chemistry.EduRules;
import edu.chemistry.photo.PhotoHooks;
import edu.chemistry.registry.EduItems;
import edu.chemistry.registry.EduSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A camera set up on its tripod. Use it and it turns to watch you, flashes like lit TNT for a few seconds, takes
 * your picture from its own lens, then vanishes in a puff.
 * Like Education's: 2 hearts, falls, hurt but never killed by fire, gone in a puff when broken, and drops nothing.
 */
public class TripodCameraEntity extends Entity {
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_TIMER =
        net.minecraft.network.syncher.SynchedEntityData.defineId(TripodCameraEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_TARGET =
        net.minecraft.network.syncher.SynchedEntityData.defineId(TripodCameraEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_SHOTS =
        net.minecraft.network.syncher.SynchedEntityData.defineId(TripodCameraEntity.class, net.minecraft.network.syncher.EntityDataSerializers.INT);
    /** how long the camera counts down (flashing) before it takes the picture */
    public static final int COUNTDOWN = 60;

    private final Lerp interpolation = new Lerp(this, 3);
    private float health = 4.0F;
    /** pictures this client has already taken for (so each shot is only taken once) */
    private int seenShots = -1;
    /** ticks until the camera goes away after taking its picture (-1 = not going) */
    private int despawn = -1;

    public TripodCameraEntity(EntityType<? extends TripodCameraEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        builder.define(DATA_TIMER, 0);
        builder.define(DATA_TARGET, -1);
        builder.define(DATA_SHOTS, 0);
    }

    /** ticks left before the picture (0 when idle) */
    public int getTimer() {
        return this.entityData.get(DATA_TIMER);
    }

    private @Nullable Player target() {
        return this.level().getEntity(this.entityData.get(DATA_TARGET)) instanceof Player p ? p : null;
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.interpolation.moveTo(x, y, z, yRot, xRot);
    }

    @Override
    public double lerpTargetX() {
        return this.interpolation.targetX();
    }

    @Override
    public double lerpTargetY() {
        return this.interpolation.targetY();
    }

    @Override
    public double lerpTargetZ() {
        return this.interpolation.targetZ();
    }

    @Override
    public void tick() {
        super.tick();
        Player target = this.target();
        if (this.level().isClientSide()) {
            this.interpolation.interpolate();
            // once clicked, a wisp of smoke rises from the top of the camera (like a torch's) until it takes the picture
            if (this.getTimer() > 0 && this.random.nextInt(4) == 0) {
                this.level().addParticle(ParticleTypes.SMOKE, this.getX(), this.getY() + 29.0 / 16.0 + 0.05, this.getZ(), 0.0, 0.0, 0.0);
            }
            int shots = this.entityData.get(DATA_SHOTS);
            if (this.seenShots < 0) this.seenShots = shots;
            if (shots != this.seenShots) {
                this.seenShots = shots;
                // the countdown finished: the player who started it gets the picture, taken through this camera
                if (target != null && target.isLocalPlayer() && EduRules.clientEnabled) {
                    this.faceToward(target);
                    PhotoHooks.takePicture.accept(false, this);
                }
            }
            return;
        }
        int timer = this.getTimer();
        if (timer > 0) {
            if (target == null || target.isRemoved() || target.distanceToSqr(this) > 64.0 * 64.0) {
                this.entityData.set(DATA_TIMER, 0);
            } else {
                // keep watching the player while counting down
                this.faceToward(target);
                if (timer == 1) {
                    this.level().playSound(null, this.getX(), this.getEyeY(), this.getZ(), EduSounds.CAMERA_TAKE_PICTURE, SoundSource.PLAYERS, 1.0F, 1.0F);
                    this.entityData.set(DATA_SHOTS, this.entityData.get(DATA_SHOTS) + 1);
                    // like Education's, the camera vanishes once it has taken the picture (a moment later, so the
                    // player's game can still look through it for the shot)
                    this.despawn = 20;
                }
                this.entityData.set(DATA_TIMER, timer - 1);
            }
        }
        if (this.despawn > 0 && --this.despawn == 0 && this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 1.2, this.getZ(), 12, 0.2, 0.4, 0.2, 0.02);
            this.discard();
            return;
        }
        Vec3 v = this.getDeltaMovement();
        if (!this.onGround()) v = v.add(0.0, -0.08, 0.0);
        this.setDeltaMovement(v);
        this.move(MoverType.SELF, v);
        this.setDeltaMovement(this.onGround() ? Vec3.ZERO : this.getDeltaMovement().multiply(0.0, 0.98, 0.0));
    }

    /** turn the camera to look at this player's face */
    public void faceToward(Player player) {
        Vec3 d = player.getEyePosition().subtract(this.getEyePosition());
        float yaw = (float) (Mth.atan2(d.z, d.x) * Mth.RAD_TO_DEG) - 90.0F;
        float pitch = (float) -(Mth.atan2(d.y, d.horizontalDistance()) * Mth.RAD_TO_DEG);
        this.setYRot(yaw);
        this.setXRot(Mth.clamp(pitch, -60.0F, 60.0F));
        this.yRotO = this.getYRot();
        this.xRotO = this.getXRot();
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level().isClientSide()) return EduRules.clientEnabled ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        if (this.getTimer() == 0 && this.despawn < 0) {
            this.entityData.set(DATA_TARGET, player.getId());
            this.entityData.set(DATA_TIMER, COUNTDOWN);
            this.faceToward(player);
            this.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.6F, 1.6F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {
        if (!(this.level() instanceof ServerLevel level) || this.isRemoved()) return false;
        if (this.isInvulnerableTo(source)) return false;
        // fire, lava and campfires hurt it but never break it
        if (source.is(DamageTypeTags.IS_FIRE)) return false;
        this.health -= damage;
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ARMOR_STAND_HIT, SoundSource.NEUTRAL, 1.0F, 1.0F);
        if (this.health <= 0.0F || (source.getEntity() instanceof Player p && p.getAbilities().instabuild)) {
            level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 1.0, this.getZ(), 12, 0.25, 0.5, 0.25, 0.02);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_DEATH, SoundSource.NEUTRAL, 1.0F, 1.0F);
            this.discard();
        }
        return true;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(EduItems.CAMERA);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.08;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.health = (tag.contains("Health") ? tag.getFloat("Health") : 4.0F);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putFloat("Health", this.health);
    }
}
