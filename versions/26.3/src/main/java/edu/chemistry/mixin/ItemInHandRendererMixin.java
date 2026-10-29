package edu.chemistry.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import edu.chemistry.client.photo.PhotoRender;
import edu.chemistry.item.GlowStickItem;
import edu.chemistry.item.SparklerItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Glow sticks and sparklers are raised like food while they're being activated, but shaken side to side instead of
 * bobbing up and down. Photos are held like maps: in both hands when the other hand is empty, otherwise in one hand,
 * with the picture on the map's paper.
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class ItemInHandRendererMixin {
    @Inject(method = "applyEatTransform", at = @At("HEAD"), cancellable = true)
    private void edu$shakeSideways(PoseStack poseStack, float frameInterp, HumanoidArm arm, float useItemRemainingTicks, int useDuration,
                                   CallbackInfo ci) {
        if (!(Minecraft.getInstance().getCameraEntity() instanceof LivingEntity user)) return;
        ItemStack stack = user.getUseItem();
        if (!(stack.getItem() instanceof GlowStickItem) && !(stack.getItem() instanceof SparklerItem)) return;
        float time = useItemRemainingTicks - frameInterp + 1.0F;
        float progress = time / Math.max(1, useDuration);
        int invert = arm == HumanoidArm.RIGHT ? 1 : -1;
        // same size of shake as eating, but left and right
        float shake = Mth.cos(time / 4.0F * (float) Math.PI) * 0.1F;
        poseStack.translate(shake, 0.0F, 0.0F);
        // and raised in front of you the way food is
        float lift = 1.0F - (float) Math.pow(progress, 27.0);
        poseStack.translate(lift * 0.6F * invert, lift * -0.5F, 0.0F);
        poseStack.rotateDegrees(Axis.YP, invert * lift * 90.0F);
        poseStack.rotateDegrees(Axis.XP, lift * 10.0F);
        poseStack.rotateDegrees(Axis.ZP, invert * lift * 30.0F);
        ci.cancel();
    }

    /**
     * a photo goes down the map's path (two hands, or one beside the other item). The game asks whether the item has a
     * map id; NeoForge's game asks whether it's a map item instead, so one of these two applies.
     */
    @Group(name = "edu$photoHeldLikeMap", min = 1)
    @Redirect(method = "submitArmWithItem", require = 0, at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/ItemStack;has(Lnet/minecraft/core/component/DataComponentType;)Z"))
    private boolean edu$photoHeldLikeMap(ItemStack stack, DataComponentType<?> type) {
        return stack.has(type) || type == DataComponents.MAP_ID && PhotoRender.of(stack) != null;
    }

    @Group(name = "edu$photoHeldLikeMap", min = 1)
    @Redirect(method = "submitArmWithItem", require = 0, at = @At(value = "CONSTANT", args = "classValue=net/minecraft/world/item/MapItem"))
    private boolean edu$photoHeldLikeMapItem(Object item, Class<?> type, net.minecraft.client.renderer.state.level.PlayerRenderState playerState,
                                              net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState state, float partialTicks,
                                              float xRot, net.minecraft.world.InteractionHand hand, float attack, ItemStack stack) {
        return type.isInstance(item) || PhotoRender.of(stack) != null;
    }

    /** and where the map's paper and contents would be drawn, the photo's picture is */
    @Inject(method = "renderMap", at = @At("HEAD"), cancellable = true)
    private void edu$drawHeldPhoto(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, ItemStack stack, boolean mainHand,
                                   FirstPersonHandsAndItemsRenderState state, CallbackInfo ci) {
        PhotoRender.Picture picture = PhotoRender.of(stack);
        if (picture == null) return;
        PhotoRender.inHand(poseStack, submitNodeCollector, lightCoords, picture);
        ci.cancel();
    }
}
