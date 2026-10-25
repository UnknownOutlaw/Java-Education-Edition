package edu.chemistry.agent;

import edu.chemistry.EduPermissions;
import edu.chemistry.EduRules;
import edu.chemistry.registry.EduItems;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Education's Agent: a small robot that belongs to one player and does what the /agent commands tell it, one block
 * at a time. Like Education's it can't be hurt, burned or pushed, floats wherever it's put, has a 27-slot inventory
 * only its owner can open, shows its owner's name in blue, and keeps the ground around it loaded while its owner is
 * online so it can work far away.
 * <p>
 * Every action happens at once on the server (so a function's next command already sees the result); players see the
 * Agent glide to its new spot, swing its arm when it works, and shrug when something can't be done.
 */
public class AgentEntity extends Entity {
    public static final int SLOTS = 27;
    /** what the Agent's last move was, for its animation: the kind in the top byte, a counter in the rest */
    private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(AgentEntity.class, EntityDataSerializers.INT);
    public static final int ANIM_SWING = 2;
    public static final int ANIM_SHRUG = 3;
    /** Education's Agent hits for 2.5 hearts */
    private static final float ATTACK_DAMAGE = 5.0F;

    /** one step glides over a quarter of a second; a teleport (anything further than a few blocks) jumps there */
    private final edu.chemistry.entity.Lerp interpolation = new edu.chemistry.entity.Lerp(this, 5);
    private final SimpleContainer inventory = new SimpleContainer(SLOTS) {
        @Override
        public boolean stillValid(Player player) {
            return AgentEntity.this.isAlive() && player.distanceToSqr(AgentEntity.this) <= 64.0;
        }
    };
    private @Nullable UUID owner;
    private String ownerName = "";
    private int actions;
    private int upkeep;

    // client: animation state
    private int seenAction = -1;
    public int animKind;
    public float animStart = -1000.0F;
    public float walkDist;
    public float walkDistO;
    public float moveSpeed;
    public float moveSpeedO;
    /** the head turns to watch a player within 6 blocks; degrees, from the way the body faces */
    public float headYaw;
    public float headYawO;
    public float headPitch;
    public float headPitchO;
    /** nothing solid under it */
    public boolean hovering;

    public AgentEntity(EntityType<? extends AgentEntity> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_ACTION, 0);
    }

    // ---------------------------------------------------------------- owner

    public @Nullable UUID owner() {
        return this.owner;
    }

    public void setOwner(ServerPlayer player) {
        this.owner = player.getUUID();
        this.ownerName = player.getGameProfile().getName();
        // Education shows the owner's name over the Agent, in blue
        this.setCustomName(Component.literal(this.ownerName).withStyle(net.minecraft.ChatFormatting.BLUE));
        this.setCustomNameVisible(true);
    }

    public boolean isOwner(Player player) {
        return this.owner != null && this.owner.equals(player.getUUID());
    }

    public @Nullable ServerPlayer ownerPlayer() {
        if (this.owner == null || !(this.level() instanceof ServerLevel server)) return null;
        return server.getServer().getPlayerList().getPlayer(this.owner);
    }

    public SimpleContainer inventory() {
        return this.inventory;
    }

    /** the way the Agent faces (it only ever faces north, east, south or west) */
    public Direction facing() {
        return Direction.fromYRot(this.getYRot());
    }

    public void face(Direction direction) {
        this.setYRot(direction.toYRot());
    }

    // ---------------------------------------------------------------- ticking

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        if (this.distanceToSqr(x, y, z) > 16.0) {
            this.setPos(x, y, z);
            this.setRot(yRot, xRot);
            this.xo = x;
            this.yo = y;
            this.zo = z;
            return;
        }
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
        if (this.level().isClientSide()) {
            this.interpolation.interpolate();
            this.clientTick();
            return;
        }
        // gone through a portal this tick: the copy in the other dimension carries on from here
        if (this.isRemoved()) return;
        ServerLevel server = (ServerLevel) this.level();
        Agents.applyPending(this, server);
        edu.chemistry.util.BlockTriggers.touch(this);
        if (--this.upkeep <= 0) {
            this.upkeep = 20;
            Agents.upkeep(this, server);
        }
    }

    private void clientTick() {
        double dx = this.getX() - this.xo;
        double dy = this.getY() - this.yo;
        double dz = this.getZ() - this.zo;
        float moved = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        this.walkDistO = this.walkDist;
        // the arm and leg swing goes through one cycle every two blocks
        this.walkDist += moved * 6.5F;
        this.moveSpeedO = this.moveSpeed;
        this.moveSpeed += (Math.min(1.0F, moved * 5.0F) - this.moveSpeed) * 0.4F;
        BlockPos below = this.blockPosition().below();
        this.hovering = this.level().getBlockState(below).getCollisionShape(this.level(), below).isEmpty();
        float lookYaw = 0.0F;
        float lookPitch = 0.0F;
        Player near = this.level().getNearestPlayer(this, 6.0);
        if (near != null && !near.isSpectator()) {
            double lx = near.getX() - this.getX();
            double lz = near.getZ() - this.getZ();
            double ly = near.getEyeY() - (this.getY() + 0.6);
            float yaw = (float) (net.minecraft.util.Mth.atan2(lz, lx) * net.minecraft.util.Mth.RAD_TO_DEG) - 90.0F;
            lookYaw = net.minecraft.util.Mth.clamp(net.minecraft.util.Mth.wrapDegrees(yaw - this.getYRot()), -85.0F, 85.0F);
            lookPitch = net.minecraft.util.Mth.clamp((float) -(net.minecraft.util.Mth.atan2(ly, Math.sqrt(lx * lx + lz * lz))
                * net.minecraft.util.Mth.RAD_TO_DEG), -40.0F, 40.0F);
        }
        this.headYawO = this.headYaw;
        this.headPitchO = this.headPitch;
        this.headYaw = net.minecraft.util.Mth.approachDegrees(this.headYaw, lookYaw, 12.0F);
        this.headPitch += net.minecraft.util.Mth.clamp(lookPitch - this.headPitch, -8.0F, 8.0F);
        int action = this.entityData.get(DATA_ACTION);
        if (this.seenAction == -1) {
            this.seenAction = action;
        } else if (action != this.seenAction) {
            this.seenAction = action;
            this.animKind = action >>> 24;
            this.animStart = this.tickCount;
        }
    }

    private void animate(int kind) {
        this.actions = (this.actions + 1) & 0xFFFFFF;
        this.entityData.set(DATA_ACTION, (kind << 24) | this.actions);
    }

    /** the arm swing that goes with getting something done */
    public void swing() {
        this.animate(ANIM_SWING);
    }

    /** Education's shrug when an action can't be done */
    public void shrug() {
        this.animate(ANIM_SHRUG);
    }

    // ---------------------------------------------------------------- directions

    /** forward / back / left / right turn with the Agent; up and down don't */
    public Direction toWorld(AgentDirection direction) {
        Direction f = this.facing();
        return switch (direction) {
            case FORWARD -> f;
            case BACK -> f.getOpposite();
            case LEFT -> f.getCounterClockWise();
            case RIGHT -> f.getClockWise();
            case UP -> Direction.UP;
            case DOWN -> Direction.DOWN;
        };
    }

    public BlockPos target(AgentDirection direction) {
        return this.blockPosition().relative(this.toWorld(direction));
    }

    private boolean inWorld(BlockPos pos) {
        return !this.level().isOutsideBuildHeight(pos) && this.level().getWorldBorder().isWithinBounds(pos);
    }

    /** the Agent builds and digs for its owner, so it can only change blocks its owner could */
    private boolean mayChange(BlockPos pos) {
        ServerPlayer owner = this.ownerPlayer();
        if (owner == null || !this.inWorld(pos) || !owner.mayBuild()) return false;
        if (!this.level().mayInteract(owner, pos)) return false;
        return EduPermissions.canBuild(this.level(), owner, pos);
    }

    // ---------------------------------------------------------------- actions (server)

    public boolean move(AgentDirection direction) {
        BlockPos to = this.target(direction);
        if (!this.inWorld(to) || !this.level().getBlockState(to).getCollisionShape(this.level(), to).isEmpty()) return false;
        ServerPlayer owner = this.ownerPlayer();
        boolean builder = owner != null && EduPermissions.isWorldBuilder(owner);
        if (!builder && EduPermissions.isBorderColumn(this.level(), to.getX(), to.getZ())) return false;
        this.setPos(to.getX() + 0.5, to.getY(), to.getZ() + 0.5);
        Agents.moved(this);
        return true;
    }

    public void turn(boolean right) {
        this.face(right ? this.facing().getClockWise() : this.facing().getCounterClockWise());
    }

    public boolean destroy(AgentDirection direction) {
        BlockPos pos = this.target(direction);
        BlockState state = this.level().getBlockState(pos);
        // it digs out anything a player could (not bedrock, barriers or command blocks), but never the permission blocks
        if (state.isAir() || state.getBlock() instanceof net.minecraft.world.level.block.LiquidBlock) return false;
        if (EduPermissions.isPermissionBlock(state.getBlock()) || state.getDestroySpeed(this.level(), pos) < 0.0F) return false;
        if (!this.mayChange(pos) || !(this.level() instanceof ServerLevel server)) return false;
        BlockEntity be = server.getBlockEntity(pos);
        Block.dropResources(state, server, pos, be, this, toolFor(state));
        server.destroyBlock(pos, false, this);
        this.swing();
        return true;
    }

    /** the tool the Agent digs with: whatever suits the block (its drops are as if a player mined it properly) */
    private static ItemStack toolFor(BlockState state) {
        if (state.is(BlockTags.MINEABLE_WITH_AXE)) return new ItemStack(Items.DIAMOND_AXE);
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)) return new ItemStack(Items.DIAMOND_SHOVEL);
        if (state.is(BlockTags.MINEABLE_WITH_HOE)) return new ItemStack(Items.DIAMOND_HOE);
        return new ItemStack(Items.DIAMOND_PICKAXE);
    }

    /** places the block in this slot (0-26) */
    public boolean place(int slot, AgentDirection direction) {
        ItemStack stack = this.inventory.getItem(slot);
        if (!(stack.getItem() instanceof BlockItem blockItem)) return false;
        BlockPos pos = this.target(direction);
        if (!this.mayChange(pos) || !this.level().getBlockState(pos).canBeReplaced()) return false;
        // like a player, only a World Builder's Agent can place Allow, Deny and Border blocks
        ServerPlayer owner = this.ownerPlayer();
        if (EduPermissions.isPermissionBlock(blockItem.getBlock()) && EduRules.enabled(this.level())
            && (owner == null || !EduPermissions.isWorldBuilder(owner))) return false;
        Direction toward = this.toWorld(direction);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), toward.getOpposite(), pos, false);
        InteractionResult result = blockItem.place(new Placement(this.level(), stack, hit, this.facing(), toward));
        if (!result.consumesAction()) return false;
        this.inventory.setChanged();
        this.swing();
        return true;
    }

    /** a block placed by the Agent faces the way the Agent does */
    private static final class Placement extends BlockPlaceContext {
        private final Direction facing;
        private final Direction toward;

        Placement(Level level, ItemStack stack, BlockHitResult hit, Direction facing, Direction toward) {
            super(level, null, InteractionHand.MAIN_HAND, stack, hit);
            this.facing = facing;
            this.toward = toward;
        }

        @Override
        public Direction getHorizontalDirection() {
            return this.facing;
        }

        @Override
        public float getRotation() {
            return this.facing.toYRot();
        }

        @Override
        public Direction getNearestLookingDirection() {
            return this.toward;
        }

        @Override
        public Direction getNearestLookingVerticalDirection() {
            return this.toward == Direction.UP ? Direction.UP : Direction.DOWN;
        }

        @Override
        public Direction[] getNearestLookingDirections() {
            Direction[] out = new Direction[6];
            out[0] = this.toward;
            int i = 1;
            for (Direction d : new Direction[] {this.facing, this.getNearestLookingVerticalDirection(), this.facing.getClockWise(),
                this.facing.getCounterClockWise(), this.facing.getOpposite(), this.toward.getOpposite(), Direction.UP, Direction.DOWN}) {
                boolean seen = false;
                for (int k = 0; k < i; k++) seen |= out[k] == d;
                if (!seen && i < 6) out[i++] = d;
            }
            return out;
        }
    }

    /** uses the block there the way its player would by right-clicking it (doors, trapdoors, buttons, levers...) */
    public boolean interactWith(AgentDirection direction) {
        BlockPos pos = this.target(direction);
        ServerPlayer owner = this.ownerPlayer();
        if (owner == null || !this.inWorld(pos) || !this.level().mayInteract(owner, pos)) return false;
        BlockState state = this.level().getBlockState(pos);
        if (state.isAir()) return false;
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), this.toWorld(direction).getOpposite(), pos, false);
        if (!state.useWithoutItem(this.level(), owner, hit).consumesAction()) return false;
        this.swing();
        return true;
    }

    /** turns grass or dirt into farmland (and coarse or rooted dirt into dirt), like using a hoe */
    public boolean till(AgentDirection direction) {
        BlockPos pos = this.target(direction);
        BlockState state = this.level().getBlockState(pos);
        if (!this.mayChange(pos)) return false;
        BlockState result = null;
        boolean airAbove = this.level().getBlockState(pos.above()).isAir();
        if ((state.is(Blocks.GRASS_BLOCK) || state.is(Blocks.DIRT) || state.is(Blocks.DIRT_PATH)) && airAbove) {
            result = Blocks.FARMLAND.defaultBlockState();
        } else if (state.is(Blocks.COARSE_DIRT) || state.is(Blocks.ROOTED_DIRT)) {
            result = Blocks.DIRT.defaultBlockState();
            if (state.is(Blocks.ROOTED_DIRT)) Block.popResourceFromFace(this.level(), pos, this.toWorld(direction).getOpposite(), new ItemStack(Items.HANGING_ROOTS));
        }
        if (result == null) return false;
        this.level().setBlock(pos, result, Block.UPDATE_ALL_IMMEDIATE);
        this.level().playSound(null, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0F, 1.0F);
        this.swing();
        return true;
    }

    public boolean attack(AgentDirection direction) {
        if (!(this.level() instanceof ServerLevel server)) return false;
        ServerPlayer owner = this.ownerPlayer();
        List<Entity> targets = server.getEntities(this, new AABB(this.target(direction)),
            e -> e instanceof LivingEntity && e.isAlive() && e != owner && !e.isSpectator());
        this.swing();
        if (targets.isEmpty()) return false;
        Entity target = targets.getFirst();
        DamageSource source = owner != null ? server.damageSources().playerAttack(owner) : server.damageSources().generic();
        return target.hurt(source, ATTACK_DAMAGE);
    }

    /** picks up the dropped items next to the Agent (all of them, or just this item) */
    public boolean collect(@Nullable Item only) {
        Predicate<ItemEntity> wanted = e -> e.isAlive() && !e.getItem().isEmpty() && (only == null || e.getItem().is(only));
        boolean any = false;
        for (ItemEntity item : this.level().getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(1.0), wanted)) {
            ItemStack before = item.getItem();
            ItemStack rest = this.inventory.addItem(before.copy());
            if (rest.getCount() == before.getCount()) continue;
            any = true;
            if (rest.isEmpty()) item.discard();
            else item.setItem(rest);
        }
        if (any) this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.NEUTRAL, 0.2F, 1.4F);
        return any;
    }

    /** drops up to {@code count} items from this slot (0-26) one block away */
    public boolean drop(int slot, int count, AgentDirection direction) {
        ItemStack taken = this.inventory.removeItem(slot, count);
        if (taken.isEmpty()) return false;
        this.spawnDrop(taken, direction);
        this.swing();
        return true;
    }

    public boolean dropAll(AgentDirection direction) {
        boolean any = false;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = this.inventory.removeItemNoUpdate(i);
            if (stack.isEmpty()) continue;
            this.spawnDrop(stack, direction);
            any = true;
        }
        if (any) {
            this.inventory.setChanged();
            this.swing();
        }
        return any;
    }

    private void spawnDrop(ItemStack stack, AgentDirection direction) {
        Vec3 at = Vec3.atBottomCenterOf(this.target(direction)).add(0.0, 0.25, 0.0);
        ItemEntity item = new ItemEntity(this.level(), at.x, at.y, at.z, stack, 0.0, 0.1, 0.0);
        item.setDefaultPickUpDelay();
        this.level().addFreshEntity(item);
    }

    /** is there a block in the way (one you can't walk through)? */
    public boolean detect(AgentDirection direction) {
        BlockPos pos = this.target(direction);
        return !this.level().getBlockState(pos).getCollisionShape(this.level(), pos).isEmpty();
    }

    /** is there a redstone signal at that block (powering it, or coming from it)? */
    public boolean detectRedstone(AgentDirection direction) {
        BlockPos pos = this.target(direction);
        if (this.level().hasNeighborSignal(pos)) return true;
        BlockState state = this.level().getBlockState(pos);
        if (!state.isSignalSource()) return false;
        for (Direction d : Direction.values()) {
            if (state.getSignal(this.level(), pos, d) > 0) return true;
        }
        return false;
    }

    /** the name of the block there, the way Education gives it (no "minecraft:" in front) */
    public String inspect(AgentDirection direction) {
        return shortId(BuiltInRegistries.BLOCK.getKey(this.level().getBlockState(this.target(direction)).getBlock()).toString());
    }

    /** Java blocks have no data values: their block state stands in (or 0 when there isn't one) */
    public String inspectData(AgentDirection direction) {
        BlockState state = this.level().getBlockState(this.target(direction));
        StringBuilder out = new StringBuilder();
        for (var property : state.getProperties()) {
            if (!out.isEmpty()) out.append(',');
            out.append(property.getName()).append('=').append(valueName(state, property));
        }
        return out.isEmpty() ? "0" : out.toString();
    }

    private static <T extends Comparable<T>> String valueName(BlockState state, net.minecraft.world.level.block.state.properties.Property<T> property) {
        return property.getName(state.getValue(property));
    }

    public static String shortId(String id) {
        return id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
    }

    /** moves up to {@code count} items from one slot to another (0-26); only onto the same item or an empty slot */
    public boolean transfer(int from, int count, int to) {
        if (from == to) return false;
        ItemStack src = this.inventory.getItem(from);
        if (src.isEmpty()) return false;
        ItemStack dst = this.inventory.getItem(to);
        int n = Math.min(count, src.getCount());
        if (dst.isEmpty()) {
            this.inventory.setItem(to, src.split(n));
        } else if (ItemStack.isSameItemSameComponents(src, dst)) {
            n = Math.min(n, dst.getMaxStackSize() - dst.getCount());
            if (n <= 0) return false;
            dst.grow(n);
            src.shrink(n);
        } else {
            return false;
        }
        this.inventory.setChanged();
        return true;
    }

    // ---------------------------------------------------------------- the entity itself

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level().isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        if (!EduRules.enabled(this.level())) {
            EduRules.tellDisabled(sp);
            return InteractionResult.FAIL;
        }
        // like Education's, only the Agent's own player can open its inventory
        if (!this.isOwner(player)) return InteractionResult.PASS;
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> ChestMenu.threeRows(id, inv, this.inventory),
            Component.translatable("entity.edu.agent")));
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {
        return false;
    }

    @Override
    public void kill() {
        // removed by /kill: its things are left behind rather than lost
        if (this.level() instanceof ServerLevel level) Containers.dropContents(level, this.blockPosition(), this.inventory);
        super.kill();
    }

    @Override
    public void remove(RemovalReason reason) {
        if ((reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED) && !this.level().isClientSide()) Agents.forget(this);
        super.remove(reason);
    }

    @Override
    public boolean ignoreExplosion(Explosion explosion) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(EduItems.AGENT_SPAWN_EGG);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        this.ownerName = tag.getString("OwnerName");
        this.inventory.getItems().clear();
        ContainerHelper.loadAllItems(tag, this.inventory.getItems(), this.registryAccess());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.owner != null) tag.putUUID("Owner", this.owner);
        tag.putString("OwnerName", this.ownerName);
        ContainerHelper.saveAllItems(tag, this.inventory.getItems(), this.registryAccess());
    }
}
