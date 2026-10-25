package edu.chemistry.mixin;

import edu.chemistry.client.photo.PictureTaker;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * On the picture's own frames nothing 2D is drawn at all: no HUD, open screens (chests, tables...), subtitles,
 * toasts or chat - just the world.
 */
@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void edu$noGuiInPicture(DeltaTracker deltaTracker, boolean shouldRenderLevel, boolean resourcesLoaded, CallbackInfo ci) {
        if (PictureTaker.hideThisFrame()) {
            var state = Minecraft.getInstance().gameRenderer.gameRenderState().guiRenderState;
            state.reset();
            state.isHudHidden = true;
        }
    }
}
