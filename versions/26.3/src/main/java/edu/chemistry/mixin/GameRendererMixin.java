package edu.chemistry.mixin;

import edu.chemistry.client.photo.PictureTaker;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The camera's picture: each frame starts by working out whether it's one of the picture's frames, and ends by taking it. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "update", at = @At("HEAD"))
    private void edu$startFrame(DeltaTracker deltaTracker, CallbackInfo ci) {
        PictureTaker.beforeFrame();
        // with Legacy4J (ReConsole): the console creative tabs
        edu.chemistry.client.legacy.LegacyCreative.sync();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void edu$takePicture(CallbackInfo ci) {
        PictureTaker.afterFrame();
    }
}
