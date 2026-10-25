package edu.chemistry.entity;

import edu.chemistry.registry.EduEntities;
import edu.chemistry.registry.EduItems;
import edu.chemistry.registry.EduSounds;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A floating balloon. It can be tied to a fence or wall, or to a mob. Light mobs get carried away; heavier ones keep it
 * hovering over them. Arrows, punches or bumping into a ceiling pop it.
 */
public class BalloonEntity extends Entity implements net.minecraft.world.entity.Leashable {
    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(BalloonEntity.class, EntityDataSerializers.INT);
    /** tied to a wall (vanilla's lead knot only goes on fences, so walls get the balloon's own knot) */
    private static final EntityDataAccessor<Optional<BlockPos>> DATA_KNOT = SynchedEntityData.defineId(BalloonEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);

    /** distance between the balloon and what it is tied to */
    public static final double STRING_LENGTH = 1.6;
    private static final int FREE_LIFETIME = 600;
    private static final int LIFT_HEIGHT = 40;

    /** the real lead: held by the mob it's tied to, or by a vanilla lead knot on a fence */
    private net.minecraft.world.entity.Leashable.@Nullable LeashData leashData;
    /** a balloon saved by an older version, still to be tied back up */
    private @Nullable UUID legacyAttached;
    private @Nullable BlockPos legacyKnot;
    private int freeTicks;
    private double liftStartY = Double.NaN;
    private double fallStartY = Double.NaN;
    /** how far a punch has knocked a tied balloon away from its resting spot; springs back over time */
    private Vec3 swing = Vec3.ZERO;

    public BalloonEntity(EntityType<? extends BalloonEntity> type, Level level) {
        super(type, level);
        this.noPhysics = false;
    }

    public static BalloonEntity create(Level level, DyeColor color, Vec3 pos) {
        BalloonEntity balloon = new BalloonEntity(EduEntities.BALLOON, level);
        balloon.setColor(color);
        balloon.setPos(pos);
        return balloon;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_COLOR, DyeColor.WHITE.getId());
        builder.define(DATA_KNOT, Optional.empty());
    }

    public DyeColor getColor() {
        return DyeColor.byId(this.entityData.get(DATA_COLOR));
    }

    public void setColor(DyeColor color) {
        this.entityData.set(DATA_COLOR, color.getId());
    }

    public void attachTo(Entity entity) {
        this.detach();
        this.setLeashedTo(entity, true);
        this.liftStartY = Double.NaN;
        this.fallStartY = Double.NaN;
        this.setPos(entity.getX(), entity.getY() + entity.getBbHeight() + STRING_LENGTH, entity.getZ());
        if (!isLight(entity)) {
            // the balloon reaches the end of its string and yanks a heavy mob up a fraction of a block
            entity.setDeltaMovement(entity.getDeltaMovement().add(0.0, 0.3, 0.0));
            entity.syncVelocity = true;
        }
    }

    /** Ties the balloon to a fence (with a real lead knot) or a wall. */
    public void attachTo(BlockPos knot) {
        this.detach();
        if (this.level().getBlockState(knot).is(BlockTags.FENCES)) {
            this.setLeashedTo(net.minecraft.world.entity.decoration.LeashFenceKnotEntity.getOrCreateKnot(this.level(), knot), true);
        } else {
            this.entityData.set(DATA_KNOT, Optional.of(knot.immutable()));
        }
        this.setPos(knot.getX() + 0.5, knot.getY() + 1.0 + STRING_LENGTH, knot.getZ() + 0.5);
    }

    public void detach() {
        if (this.isLeashed()) this.removeLeash();
        this.entityData.set(DATA_KNOT, Optional.empty());
        this.freeTicks = 0;
    }

    /** the mob (or boat) the balloon is tied to, if any */
    public @Nullable Entity getAttachedEntity() {
        Entity holder = this.getLeashHolder();
        return holder instanceof net.minecraft.world.entity.decoration.LeashFenceKnotEntity ? null : holder;
    }

    /** the fence or wall the balloon is tied to, if any */
    public Optional<BlockPos> getKnot() {
        if (this.getLeashHolder() instanceof net.minecraft.world.entity.decoration.LeashFenceKnotEntity knot) return Optional.of(knot.getPos());
        return this.entityData.get(DATA_KNOT);
    }

    public boolean isTied() {
        return this.isLeashed() || this.entityData.get(DATA_KNOT).isPresent();
    }

    /** where in its sway this balloon starts (0..2pi), so balloons next to each other don't sway in step */
    public float swayPhase() {
        return (this.getUUID().hashCode() & 0xFFFF) / 65536.0F * (float) (Math.PI * 2.0);
    }

    /** each balloon sways a little faster or slower than the others (0.8..1.2) */
    public float swayRate() {
        return 0.8F + ((this.getUUID().hashCode() >>> 16) & 0xFF) / 255.0F * 0.4F;
    }

    /** the balloons tied to the fence or wall at {@code pos} */
    public static java.util.List<BalloonEntity> balloonsOn(Level level, BlockPos pos) {
        return level.getEntitiesOfClass(BalloonEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(4.0),
            b -> b.getKnot().filter(pos::equals).isPresent());
    }

    /**
     * Clicking the lead knot on a fence or wall unties the balloons there, like a vanilla lead knot.
     * @return true if there was a balloon to untie
     */
    public static boolean untieFrom(Level level, BlockPos pos) {
        java.util.List<BalloonEntity> balloons = balloonsOn(level, pos);
        if (balloons.isEmpty()) return false;
        if (!level.isClientSide()) {
            for (BalloonEntity b : balloons) b.untie();
        }
        return true;
    }

    /** let go: the balloon rises quickly into the air */
    public void untie() {
        this.detach();
        this.setDeltaMovement(0.0, 0.8, 0.0);
        this.playSound(net.minecraft.sounds.SoundEvents.LEAD_UNTIED, 1.0F, 1.0F);
    }

    /** whether a balloon is already tied to the fence or wall at {@code pos} */
    public static boolean hasBalloonOn(Level level, BlockPos pos) {
        return !level.getEntitiesOfClass(BalloonEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(4.0),
            b -> b.getKnot().filter(pos::equals).isPresent()).isEmpty();
    }

    public static boolean hasBalloon(Entity entity) {
        return !entity.level().getEntitiesOfClass(BalloonEntity.class, entity.getBoundingBox().inflate(6.0),
            b -> b.getLeashHolder() == entity).isEmpty();
    }

    /** Bedrock's balloon mass for each mob (anything not listed counts as 1.0). */
    private static final java.util.Map<String, Float> MASS = java.util.Map.ofEntries(
        java.util.Map.entry("rabbit", 0.4F),
        java.util.Map.entry("allay", 0.5F), java.util.Map.entry("bee", 0.5F), java.util.Map.entry("glow_squid", 0.5F), java.util.Map.entry("squid", 0.5F),
        java.util.Map.entry("cat", 0.6F), java.util.Map.entry("chicken", 0.6F), java.util.Map.entry("fox", 0.6F), java.util.Map.entry("skeleton_horse", 0.6F),
        java.util.Map.entry("ocelot", 0.7F),
        java.util.Map.entry("pig", 0.75F), java.util.Map.entry("sheep", 0.75F),
        java.util.Map.entry("wolf", 0.8F),
        java.util.Map.entry("turtle", 0.9F),
        java.util.Map.entry("iron_golem", 2.0F));

    public static float mass(Entity entity) {
        return MASS.getOrDefault(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getPath(), 1.0F);
    }

    /** Mass 1 or less: the balloon carries the mob away. Heavier (iron golems) stay on the ground. */
    public static boolean isLight(Entity entity) {
        return mass(entity) <= 1.0F;
    }

    /** a heavy mob that falls further than this while held by a balloon makes the balloon despawn (Bedrock) */
    private static final double HEAVY_FALL_LIMIT = 16.0;

    /** smooth movement on the client between position updates (the level calls it each tick) */
    @Override
    protected InterpolationHandler createInterpolationHandler() {
        return net.minecraft.world.entity.LinearInterpolationHandler.create(this, 3);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        ServerLevel level = (ServerLevel) this.level();
        double bob = Math.sin(this.tickCount * 0.08 * this.swayRate() + this.swayPhase()) * 0.05;

        if (this.legacyKnot != null) {
            BlockPos legacy = this.legacyKnot;
            this.legacyKnot = null;
            BlockState st = level.getBlockState(legacy);
            if (st.is(BlockTags.FENCES) || st.is(BlockTags.WALLS)) this.attachTo(legacy);
        }
        if (this.legacyAttached != null) {
            Entity found = level.getEntity(this.legacyAttached);
            if (found != null) {
                this.legacyAttached = null;
                this.setLeashedTo(found, true);
            } else if (this.tickCount > 40) {
                this.legacyAttached = null;
            }
        }

        Optional<BlockPos> knot = this.getKnot();
        Entity attached = this.getAttachedEntity();
        if (knot.isPresent()) {
            BlockPos pos = knot.get();
            BlockState state = level.getBlockState(pos);
            if (!state.is(BlockTags.FENCES) && !state.is(BlockTags.WALLS)) {
                this.untie();
            } else {
                Vec3 target = this.swung(new Vec3(pos.getX() + 0.5, pos.getY() + 1.0 + STRING_LENGTH + bob, pos.getZ() + 0.5));
                this.setDeltaMovement(target.subtract(this.position()).scale(0.3));
                this.move(MoverType.SELF, this.getDeltaMovement());
                if (this.horizontalCollision || this.verticalCollision && this.getDeltaMovement().y > 0.01) {
                    this.pop();
                }
            }
            return;
        }

        if (attached != null) {
            if (!attached.isAlive() || attached.isRemoved() || attached.level() != level) {
                this.detach();
                return;
            }
            boolean light = isLight(attached);
            if (light) {
                if (Double.isNaN(this.liftStartY)) this.liftStartY = attached.getY();
                Vec3 m = attached.getDeltaMovement();
                attached.setDeltaMovement(m.x * 0.9, Math.max(m.y, 0.2), m.z * 0.9);
                attached.resetFallDistance();
                attached.syncVelocity = true;
                if (attached.getY() > this.liftStartY + LIFT_HEIGHT || attached.getY() > level.getMaxY()) {
                    this.floatAway(level, attached);
                    return;
                }
            } else {
                this.liftStartY = Double.NaN;
                if (attached.onGround()) {
                    this.fallStartY = Double.NaN;
                } else {
                    if (Double.isNaN(this.fallStartY)) this.fallStartY = attached.getY();
                    if (this.fallStartY - attached.getY() > HEAVY_FALL_LIMIT) {
                        // the balloon gives up once its heavy mob has fallen more than 16 blocks: it pops and the mob drops
                        this.pop();
                        return;
                    }
                    if (attached.getDeltaMovement().y < -0.08) {
                        // heavy mobs only fall slowly while a balloon holds them
                        Vec3 m = attached.getDeltaMovement();
                        attached.setDeltaMovement(m.x, -0.08, m.z);
                        attached.resetFallDistance();
                        attached.syncVelocity = true;
                    }
                }
            }
            this.followOnLead(attached, bob);
            if (this.verticalCollision && this.getDeltaMovement().y > 0.01) {
                this.pop();
            }
            return;
        }

        // free floating
        this.freeTicks++;
        Vec3 m = this.getDeltaMovement();
        this.setDeltaMovement(m.x * 0.9, Math.min(m.y + 0.06, 1.0), m.z * 0.9);
        this.move(MoverType.SELF, this.getDeltaMovement());
        if (this.verticalCollision || this.horizontalCollision) {
            this.pop();
        } else if (this.freeTicks > FREE_LIFETIME || this.getY() > level.getMaxY() + 32) {
            this.discard();
        }
    }

    /** how far the balloon can drift from over its mob before the lead goes tight and starts pulling the mob */
    private static final double LEAD_SLACK = 0.9;
    /** the furthest the balloon can get from over its mob (the lead doesn't stretch) */
    private static final double LEAD_REACH = 2.4;

    /**
     * Tied to a mob, the balloon and the mob pull on each other through the lead, like a mob being led by a player:
     * the balloon trails behind a walking mob, and when the balloon is knocked away the lead goes tight and the mob is
     * dragged along after it, lagging behind.
     */
    private void followOnLead(Entity mob, double bob) {
        double hx = this.getX() - mob.getX();
        double hz = this.getZ() - mob.getZ();
        double h = Math.sqrt(hx * hx + hz * hz);
        Vec3 v = this.getDeltaMovement();
        // the balloon drifts back over its mob, keeping some of its own momentum
        double vx = v.x * 0.88 - hx * 0.02;
        double vz = v.z * 0.88 - hz * 0.02;
        if (h > LEAD_SLACK) {
            // lead pulled tight: tug the mob toward the balloon (light mobs more than heavy ones)
            double pull = (h - LEAD_SLACK) * 0.16 / Math.max(0.4F, mass(mob));
            mob.addDeltaMovement(new Vec3(hx / h * pull, 0.0, hz / h * pull));
            mob.syncVelocity = true;
            // and the mob holds the balloon back
            vx -= hx / h * (h - LEAD_SLACK) * 0.05;
            vz -= hz / h * (h - LEAD_SLACK) * 0.05;
        }
        // further out on its lead, the balloon hangs a little lower
        double reach = Math.min(h, STRING_LENGTH);
        double drop = STRING_LENGTH - Math.sqrt(STRING_LENGTH * STRING_LENGTH - reach * reach);
        double targetY = mob.getY() + mob.getBbHeight() + STRING_LENGTH + bob - drop;
        this.setDeltaMovement(vx, (targetY - this.getY()) * 0.5, vz);
        this.move(MoverType.SELF, this.getDeltaMovement());
        // the lead doesn't stretch
        hx = this.getX() - mob.getX();
        hz = this.getZ() - mob.getZ();
        h = Math.sqrt(hx * hx + hz * hz);
        if (h > LEAD_REACH) {
            this.setPos(mob.getX() + hx / h * LEAD_REACH, this.getY(), mob.getZ() + hz / h * LEAD_REACH);
        }
    }

    /** Light mob carried high into the sky: the balloon pops and the mob falls back to the ground (nothing is removed). */
    private void floatAway(ServerLevel level, Entity attached) {
        this.pop();
    }

    public void pop() {
        if (this.level() instanceof ServerLevel level && !this.isRemoved()) {
            int rgb = this.getColor().getTextureDiffuseColor() & 0xFFFFFF;
            level.sendParticles(new DustParticleOptions(rgb, 1.0F), this.getX(), this.getY() + 0.3, this.getZ(), 16, 0.25, 0.3, 0.25, 0.0);
            level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.3, this.getZ(), 4, 0.2, 0.2, 0.2, 0.01);
            level.playSound(null, this.getX(), this.getY(), this.getZ(), EduSounds.BALLOON_POP, SoundSource.NEUTRAL, 10.0F,
                1.75F + this.random.nextFloat() * 0.25F);
            this.discard();
        }
    }

    /** Rest position of a tied balloon, pushed sideways by any recent punch (kept within the string's reach). */
    private Vec3 swung(Vec3 rest) {
        this.swing = this.swing.scale(0.9);
        if (this.swing.lengthSqr() < 1.0E-4) {
            this.swing = Vec3.ZERO;
            return rest;
        }
        Vec3 off = this.swing;
        if (off.length() > STRING_LENGTH) off = off.normalize().scale(STRING_LENGTH);
        // swinging on a string: the further out it goes, the lower it hangs
        double drop = STRING_LENGTH - Math.sqrt(Math.max(0.0, STRING_LENGTH * STRING_LENGTH - off.horizontalDistanceSqr()));
        return rest.add(off.x, off.y - drop, off.z);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.isInvulnerableToBase(source)) return false;
        Entity attacker = source.getEntity();
        if (attacker != null && source.getDirectEntity() == attacker) {
            // a punch (left click) knocks the balloon away in the direction you are facing instead of popping it
            Vec3 dir = attacker.getLookAngle();
            Vec3 push = new Vec3(dir.x, Math.max(dir.y, 0.0) * 0.5 + 0.1, dir.z).normalize();
            Entity carried = this.getAttachedEntity();
            if (carried != null) {
                // knocked away on its lead: the balloon flies off and drags its mob along behind it
                this.setDeltaMovement(this.getDeltaMovement().add(push.x * 1.6, 0.0, push.z * 1.6));
            } else if (this.isTied()) {
                this.swing = this.swing.add(push.scale(1.2));
            } else {
                this.setDeltaMovement(this.getDeltaMovement().add(push.scale(0.6)));
            }
            this.syncVelocity = true;
            level.playSound(null, this.getX(), this.getY(), this.getZ(), net.minecraft.sounds.SoundEvents.WOOL_HIT, SoundSource.NEUTRAL, 0.6F, 1.6F);
            return true;
        }
        // arrows, tridents, wind charges, explosions and the like pop it
        this.pop();
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
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (this.isTied()) {
            if (!this.level().isClientSide()) this.untie();
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // ---------------------------------------------------------------- the real lead (Leashable)

    @Override
    public net.minecraft.world.entity.Leashable.@Nullable LeashData getLeashData() {
        return this.leashData;
    }

    @Override
    public void setLeashData(net.minecraft.world.entity.Leashable.@Nullable LeashData data) {
        this.leashData = data;
    }

    /** players can't put their own lead on a balloon (it comes with its string) */
    @Override
    public boolean canBeLeashed() {
        return false;
    }

    /** the string is part of the balloon: letting go never drops a lead item */
    @Override
    public void dropLeash() {
        this.removeLeash();
    }

    /** a popped or broken balloon lets go of its lead, so a fence's lead knot doesn't stay behind on its own */
    @Override
    public void remove(Entity.RemovalReason reason) {
        if (reason.shouldDestroy() && this.isLeashed() && !this.level().isClientSide()) this.removeLeash();
        super.remove(reason);
    }

    @Override
    public void onLeashRemoved() {
        this.freeTicks = 0;
    }

    /** the lead is tied at the knot under the balloon */
    @Override
    public Vec3 getLeashOffset() {
        return Vec3.ZERO;
    }

    /** the balloon moves itself; no vanilla lead pull or snapping */
    @Override
    public boolean checkElasticInteractions(Entity holder, net.minecraft.world.entity.Leashable.LeashData data) {
        return false;
    }

    @Override
    public double leashSnapDistance() {
        return 1.0E6;
    }

    @Override
    public double leashElasticDistance() {
        return 1.0E6;
    }

    @Override
    public net.minecraft.world.entity.item.@Nullable ItemEntity spawnAtLocation(ServerLevel level, net.minecraft.world.level.ItemLike item) {
        // a balloon whose mob can't be found after loading would otherwise drop a lead
        if (item.asItem() == net.minecraft.world.item.Items.LEAD) return null;
        return super.spawnAtLocation(level, item);
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(EduItems.BALLOONS.get(this.getColor()));
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128.0 * 128.0;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        this.setColor(DyeColor.byId(input.getIntOr("Color", 0)));
        this.readLeashData(input);
        // balloons saved before they had real leads
        this.legacyAttached = input.read("Attached", UUIDUtil.CODEC).orElse(null);
        this.legacyKnot = input.read("Knot", BlockPos.CODEC).orElse(null);
        input.read("WallKnot", BlockPos.CODEC).ifPresent(pos -> this.entityData.set(DATA_KNOT, Optional.of(pos)));
        this.freeTicks = input.getIntOr("FreeTicks", 0);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("Color", this.getColor().getId());
        this.writeLeashData(output, this.leashData);
        this.entityData.get(DATA_KNOT).ifPresent(pos -> output.store("WallKnot", BlockPos.CODEC, pos));
        output.putInt("FreeTicks", this.freeTicks);
    }
}
