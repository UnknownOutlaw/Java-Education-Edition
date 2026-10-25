package edu.chemistry.mixin;

import edu.chemistry.client.photo.PictureTaker;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lets the camera take a picture on a frame of its own (no HUD, and through a tripod camera's lens when used). */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "update", at = @At("HEAD"))
    private void edu$startFrame(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        PictureTaker.beforeFrame();
        // with Legacy4J (ReConsole): the console creative tabs
        edu.chemistry.client.legacy.LegacyCreative.sync();
    }

    /**
     * On the picture's own frames nothing 2D is drawn at all: no HUD, open screens (chests, tables...), subtitles,
     * toasts or chat - just the world.
     */
    @Inject(method = "extractGui", at = @At("TAIL"))
    private void edu$noGuiInPicture(DeltaTracker deltaTracker, boolean advanceGameTime, boolean renderGui, CallbackInfo ci) {
        if (PictureTaker.hideThisFrame()) ((GameRenderer) (Object) this).getGameRenderState().guiRenderState.reset();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void edu$takePicture(DeltaTracker deltaTracker, boolean advanceGameTime, CallbackInfo ci) {
        PictureTaker.afterFrame();
    }
}
