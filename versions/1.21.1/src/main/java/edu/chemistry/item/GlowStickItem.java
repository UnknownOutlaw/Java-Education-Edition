package edu.chemistry.item;

import net.minecraft.world.InteractionResultHolder;
import edu.chemistry.registry.EduSounds;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Shake it (hold use for 1 second) to make it glow. Durability only drains while it is glowing, then it is used up. */
public class GlowStickItem extends Item {
    /** 100 durability, one point every 2 seconds = 200 seconds of glow */
    private static final int TICKS_PER_DAMAGE = 40;
    private final DyeColor color;

    public GlowStickItem(DyeColor color, Properties properties) {
        super(properties);
        this.color = color;
    }

    /** how long you have to keep shaking it (hold use) before it lights up */
    public static final int SHAKE_TICKS = 20;

    /** Hold use to shake it; once it has been shaken for a few seconds it starts glowing. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (LitState.isLit(stack)) {
            return InteractionResultHolder.pass(player.getItemInHand(hand));
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return LitState.isLit(stack) ? 0 : SHAKE_TICKS;
    }

    /** held up and bobbed like eating food */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        // Bedrock plays the glow stick "crack" (fire.ignite, pitch 0.5-0.7) while it is being activated
        if (!level.isClientSide() && (SHAKE_TICKS - remaining) % 10 == 0) {
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(), EduSounds.GLOW_STICK_USE, SoundSource.PLAYERS, 1.0F,
                0.5F + entity.getRandom().nextFloat() * 0.2F);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide()) {
            LitState.setLit(stack, true);
        }
        return stack;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slotId, boolean selected) {
        if (!(world instanceof ServerLevel level)) return;
        // which hand it's in, if any (1.21.1 only says whether it's the selected hotbar slot)
        EquipmentSlot slot = entity instanceof LivingEntity holder && holder.getOffhandItem() == stack ? EquipmentSlot.OFFHAND
            : entity instanceof LivingEntity holder2 && selected && holder2.getMainHandItem() == stack ? EquipmentSlot.MAINHAND : null;
        if (!LitState.isLit(stack)) return;
        if (level.getGameTime() % TICKS_PER_DAMAGE == 0) {
            int damage = stack.getDamageValue() + 1;
            if (damage >= stack.getMaxDamage()) {
                stack.setCount(0);
                return;
            }
            stack.setDamageValue(damage);
        }
        if ((slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) && level.getGameTime() % 4 == 0) {
            Vec3 tip = SparklerItem.handTip(entity, slot);
            level.sendParticles(new DustParticleOptions(net.minecraft.world.phys.Vec3.fromRGB24(this.color.getTextureDiffuseColor() & 0xFFFFFF).toVector3f(), 0.6F), tip.x, tip.y, tip.z, 1, 0.08, 0.08, 0.08, 0.0);
        }
    }
}
