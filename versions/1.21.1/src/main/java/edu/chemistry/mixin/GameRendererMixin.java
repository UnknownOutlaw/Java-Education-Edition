package edu.chemistry.mixin;

import edu.chemistry.client.photo.PictureTaker;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets the camera take a picture on a frame of its own (no HUD, and through a tripod camera's lens when used). The
 * picture is grabbed as soon as the world is drawn, before any screen, chat or toast is drawn on top of it.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "render", at = @At("HEAD"))
    private void edu$startFrame(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        PictureTaker.beforeFrame();
        // with Legacy4J (ReConsole): the console creative tabs
        edu.chemistry.client.legacy.LegacyCreative.sync();
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;renderLevel(Lnet/minecraft/client/DeltaTracker;)V", shift = At.Shift.AFTER))
    private void edu$afterWorld(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        PictureTaker.afterWorld();
    }

    /** the shot is set up between frames (and taken here too on a frame without a world, so it never gets stuck) */
    @Inject(method = "render", at = @At("TAIL"))
    private void edu$endFrame(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
        PictureTaker.afterFrame();
    }
}
