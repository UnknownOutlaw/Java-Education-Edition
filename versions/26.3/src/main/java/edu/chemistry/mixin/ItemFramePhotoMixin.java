package edu.chemistry.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import edu.chemistry.client.photo.PhotoFrameState;
import edu.chemistry.client.photo.PhotoRender;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.decoration.ItemFrame;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    private BlockModelResolver blockModelResolver;

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/decoration/ItemFrame;Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;F)V",
        at = @At("TAIL"))
    private void edu$extractPhoto(ItemFrame frame, ItemFrameRenderState state, float partialTicks, CallbackInfo ci) {
        PhotoRender.Picture photo = state.mapId == null ? PhotoRender.of(frame.getItem()) : null;
        ((PhotoFrameState) state).edu$setPhoto(photo);
        if (photo == null) return;
        // no small item in the middle, and the frame in its map shape
        state.item.clear();
        if (!state.isInvisible) this.blockModelResolver.updateForItemFrame(state.frameModel, state.isGlowFrame, true);
    }

    /** where a map would be drawn: after the frame and its depth offset, before the frame's transform is undone */
    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/ItemFrameRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V", ordinal = 1))
    private void edu$submitPhoto(ItemFrameRenderState state, PoseStack pose, SubmitNodeCollector out, CameraRenderState camera, CallbackInfo ci) {
        PhotoRender.Picture photo = ((PhotoFrameState) state).edu$photo();
        if (photo != null) PhotoRender.inFrame(pose, out, state.isGlowFrame ? EDU_GLOW_LIGHT : state.lightCoords, photo, state.rotation);
    }
}
