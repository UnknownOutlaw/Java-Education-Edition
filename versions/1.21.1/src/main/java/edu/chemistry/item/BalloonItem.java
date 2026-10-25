package edu.chemistry.item;

import edu.chemistry.entity.BalloonEntity;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Use on a fence or wall to tie it there, on a mob to tie it to the mob, or on any other block to let it float away. */
public class BalloonItem extends Item {
    /** mobs a balloon can't be tied to (same exceptions as Bedrock) */
    private static final Set<String> CANNOT_ATTACH = Set.of("ravager", "axolotl", "frog", "happy_ghast", "nautilus", "zombie_nautilus",
        "sulfur_cube", "ender_dragon", "wither", "ghast");

    /** mobs that take a balloon even though Java won't tie a lead to them (Bedrock's balloon mass list) */
    private static final Set<String> ALWAYS = Set.of("panda", "turtle", "cat", "wolf", "ocelot", "parrot", "fox", "squid", "glow_squid",
        "dolphin", "hoglin", "zoglin", "polar_bear", "iron_golem", "snow_golem", "copper_golem", "allay", "bee", "rabbit", "chicken");

    private final DyeColor color;

    public BalloonItem(DyeColor color, Properties properties) {
        super(properties);
        this.color = color;
    }

    public DyeColor color() {
        return this.color;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!level.isClientSide()) {
            BalloonEntity balloon;
            if (state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS)) {
                if (BalloonEntity.hasBalloonOn(level, pos)) {
                    // one balloon per fence post
                    return InteractionResult.FAIL;
                }
                balloon = BalloonEntity.create(level, this.color, Vec3.atBottomCenterOf(pos.above()));
                balloon.attachTo(pos);
                level.playSound(null, pos, SoundEvents.LEASH_KNOT_PLACE, SoundSource.BLOCKS, 1.0F, 1.0F);
            } else {
                BlockPos at = pos.relative(context.getClickedFace());
                balloon = BalloonEntity.create(level, this.color, Vec3.atBottomCenterOf(at));
            }
            level.addFreshEntity(balloon);
            context.getItemInHand().consume(1, context.getPlayer());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        return tryAttach(stack, player, target);
    }

    /** Ties a balloon from {@code stack} to {@code target} (a mob or a boat) if Bedrock would allow it. */
    public InteractionResult tryAttach(ItemStack stack, Player player, Entity target) {
        if (!edu.chemistry.EduRules.enabled(player.level())) {
            if (player instanceof net.minecraft.server.level.ServerPlayer sp) edu.chemistry.EduRules.tellDisabled(sp);
            return InteractionResult.FAIL;
        }
        if (!canAttach(target)) {
            return InteractionResult.PASS;
        }
        if (!player.level().isClientSide()) {
            BalloonEntity balloon = BalloonEntity.create(player.level(), this.color, target.position());
            balloon.attachTo(target);
            player.level().addFreshEntity(balloon);
            player.level().playSound(null, target.getX(), target.getY(), target.getZ(), SoundEvents.LEASH_KNOT_PLACE, SoundSource.NEUTRAL, 1.0F, 1.0F);
            stack.consume(1, player);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Bedrock: almost anything a lead can be tied to can take a balloon (pandas and turtles too, which can't take
     * leads), except axolotls, frogs, happy ghasts, nautiluses, zombie nautiluses, sulfur cubes and ravagers. Players
     * can't, and a mob only carries one balloon.
     */
    public static boolean canAttach(Entity target) {
        if (target instanceof Player || !target.isAlive() || target instanceof BalloonEntity) return false;
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).getPath();
        if (CANNOT_ATTACH.contains(id)) return false;
        boolean allowed = target instanceof Boat
            || target instanceof Mob mob && (ALWAYS.contains(id) || mob.canBeLeashed() && !(mob instanceof Enemy));
        return allowed && !BalloonEntity.hasBalloon(target);
    }
}
