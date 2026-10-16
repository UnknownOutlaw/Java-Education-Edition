package edu.chemistry.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import edu.chemistry.item.GlowStickItem;
import edu.chemistry.item.SparklerItem;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Glow sticks and sparklers are raised like food while they're being activated, but shaken side to side instead of
 * bobbing up and down.
 */
@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(method = "applyEatTransform", at = @At("HEAD"), cancellable = true)
    private void edu$shakeSideways(PoseStack poseStack, float frameInterp, HumanoidArm arm, ItemStack stack, Player player, CallbackInfo ci) {
        if (!(stack.getItem() instanceof GlowStickItem) && !(stack.getItem() instanceof SparklerItem)) return;
        float time = player.getUseItemRemainingTicks() - frameInterp + 1.0F;
        float progress = time / stack.getUseDuration(player);
        int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
        // same size of shake as eating, but left and right
        float shake = Mth.cos(time / 4.0F * (float) Math.PI) * 0.1F;
        poseStack.translate(shake, 0.0F, 0.0F);
        // and raised in front of you the way food is
        float lift = 1.0F - (float) Math.pow(progress, 27.0);
        poseStack.translate(lift * 0.6F * invert, lift * -0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(invert * lift * 90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(lift * 10.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(invert * lift * 30.0F));
        ci.cancel();
    }
}
