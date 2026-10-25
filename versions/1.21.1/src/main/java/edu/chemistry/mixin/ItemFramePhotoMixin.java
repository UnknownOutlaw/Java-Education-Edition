package edu.chemistry.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import edu.chemistry.client.photo.PhotoRender;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A photo in an item frame is shown like a map: the frame switches to its map shape and the picture covers its face,
 * instead of the small photo item.
 */
@Mixin(ItemFrameRenderer.class)
public abstract class ItemFramePhotoMixin {
    /** the light a glow item frame gives a map */
    private static final int EDU_GLOW_LIGHT = 15728850;

    @Shadow
    @Final
    private static ModelResourceLocation MAP_FRAME_LOCATION;

    @Shadow
    @Final
    private static ModelResourceLocation GLOW_MAP_FRAME_LOCATION;

    /** the frame in its map shape */
    @Inject(method = "getFrameModelResourceLoc", at = @At("HEAD"), cancellable = true)
    private void edu$photoFrameShape(ItemFrame frame, ItemStack stack, CallbackInfoReturnable<ModelResourceLocation> cir) {
        if (PhotoRender.of(stack) != null) cir.setReturnValue(frame.getType() == EntityType.GLOW_ITEM_FRAME ? GLOW_MAP_FRAME_LOCATION : MAP_FRAME_LOCATION);
    }

    /**
     * Where the item would be drawn, after the frame and its depth offset: the picture instead. The frame has already
     * turned the item an eighth turn per click; a map only turns in quarter turns, so that turn is undone first.
     */
    @Redirect(method = "render(Lnet/minecraft/world/entity/decoration/ItemFrame;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;renderStatic(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;IILcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/world/level/Level;I)V"))
    private void edu$drawPhoto(ItemRenderer items, ItemStack stack, ItemDisplayContext context, int light, int overlay, PoseStack pose,
                               MultiBufferSource buffers, Level level, int seed) {
        PhotoRender.Picture photo = PhotoRender.of(stack);
        ItemFrame frame = stack.getFrame();
        if (photo == null || frame == null) {
            items.renderStatic(stack, context, light, overlay, pose, buffers, level, seed);
            return;
        }
        int rotation = frame.getRotation();
        pose.mulPose(Axis.ZP.rotationDegrees(-rotation * 360.0F / 8.0F));
        PhotoRender.inFrame(pose, buffers, frame.getType() == EntityType.GLOW_ITEM_FRAME ? EDU_GLOW_LIGHT : light, photo, rotation);
    }
}
