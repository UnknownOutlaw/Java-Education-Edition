package edu.chemistry.item;

import edu.chemistry.registry.EduSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Hold use for 2 seconds to light it; it burns for 60 seconds, throwing coloured sparks. Lit sparklers are destroyed if their holder goes under water. */
public class SparklerItem extends Item {
    /** 100 durability over 60 seconds = 1 point every 12 ticks */
    private static final int TICKS_PER_DAMAGE = 12;
    private final int color;

    public SparklerItem(String color, Properties properties) {
        super(properties);
        this.color = switch (color) {
            case "orange" -> 0xFF8A1C;
            case "blue" -> 0x3C64FF;
            case "red" -> 0xFF2A2A;
            case "purple" -> 0xB23CFF;
            case "green" -> 0x3CFF6A;
            default -> 0xFFFFFF;
        };
    }

    /** how long you hold use before it catches (2 seconds) */
    public static final int LIGHT_TICKS = 40;

    /** Hold use for 2 seconds (with the eating animation) to light it. Can't be lit under water. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (LitState.isLit(stack)) {
            return InteractionResult.PASS;
        }
        if (player.isInWater()) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return LitState.isLit(stack) ? 0 : LIGHT_TICKS;
    }

    @Override
    public net.minecraft.world.item.ItemUseAnimation getUseAnimation(ItemStack stack) {
        return net.minecraft.world.item.ItemUseAnimation.EAT;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        // Bedrock's sparkler.use: random.fizz at pitch 2.4-2.6 while it is being lit
        if (!level.isClientSide() && (LIGHT_TICKS - remaining) % 10 == 0) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F,
                2.4F + entity.getRandom().nextFloat() * 0.2F);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide() && !entity.isInWater()) {
            LitState.setLit(stack, true);
        }
        return stack;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        if (!LitState.isLit(stack)) return;
        if (entity.isInWater()) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 1.6F);
            stack.setCount(0);
            return;
        }
        if (level.getGameTime() % TICKS_PER_DAMAGE == 0) {
            int damage = stack.getDamageValue() + 1;
            if (damage >= stack.getMaxDamage()) {
                stack.setCount(0);
                return;
            }
            stack.setDamageValue(damage);
        }
        if (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
            Vec3 tip = handTip(entity, slot);
            level.sendParticles(new DustParticleOptions(this.color, 0.7F), tip.x, tip.y, tip.z, 3, 0.12, 0.12, 0.12, 0.0);
            if (level.getRandom().nextInt(3) == 0) {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, tip.x, tip.y, tip.z, 1, 0.05, 0.05, 0.05, 0.05);
            }
            // the sparkle sound plays every tick while it's held (Bedrock's sparkler.active: volume 0.25, pitch 0.8-1.2)
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), EduSounds.SPARKLER_ACTIVE, SoundSource.PLAYERS, 0.25F,
                0.8F + level.getRandom().nextFloat() * 0.4F);
        }
    }

    static Vec3 handTip(Entity entity, EquipmentSlot slot) {
        float yaw = entity.getYRot() * ((float) Math.PI / 180F);
        boolean rightHanded = !(entity instanceof LivingEntity living) || living.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT;
        boolean right = (slot == EquipmentSlot.MAINHAND) == rightHanded;
        double side = right ? -0.38 : 0.38;
        double fx = -Math.sin(yaw) * 0.55;
        double fz = Math.cos(yaw) * 0.55;
        double sx = Math.cos(yaw) * side;
        double sz = Math.sin(yaw) * side;
        return new Vec3(entity.getX() + fx + sx, entity.getY() + entity.getBbHeight() * 0.72, entity.getZ() + fz + sz);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return false;
    }
}
