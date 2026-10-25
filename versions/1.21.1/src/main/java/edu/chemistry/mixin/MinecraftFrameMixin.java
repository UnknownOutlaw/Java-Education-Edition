package edu.chemistry.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import edu.chemistry.client.photo.PictureTaker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A tripod camera's picture is drawn from its lens on a couple of frames of its own; those frames are kept off the
 * screen, so your view never jumps to the camera.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftFrameMixin {
    @Redirect(method = "runTick", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/pipeline/RenderTarget;blitToScreen(II)V"))
    private void edu$keepPictureFramesOffScreen(RenderTarget target, int width, int height) {
        if (!PictureTaker.hideThisFrame()) target.blitToScreen(width, height);
    }

    /** Code Builder's key, and telling the MakeCode editor when a world is joined or left */
    @Inject(method = "tick", at = @At("TAIL"))
    private void edu$codeBuilder(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        edu.chemistry.client.code.CodeBuilderClient.tick();
    }

    /** the HUD is hidden for the picture, but everyone's name tags still show in it */
    @Inject(method = "renderNames", at = @At("HEAD"), cancellable = true)
    private static void edu$namesInPictures(CallbackInfoReturnable<Boolean> cir) {
        if (PictureTaker.hideThisFrame()) cir.setReturnValue(true);
    }
}
