package edu.chemistry.mixin;

import edu.chemistry.client.photo.PhotoToast;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the control tips, and the camera's flash and the little picture that pops up after taking one. */
@Mixin(Gui.class)
public abstract class GuiMixin {
    /**
     * after the HUD, which the game skips on the level loading screen (on every return rather than the last one: Forge
     * draws its own HUD and returns early)
     */
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void edu$photoToast(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (net.minecraft.client.Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.LevelLoadingScreen) return;
        edu.chemistry.client.ControlTips.render(graphics, graphics.guiWidth(), graphics.guiHeight());
        PhotoToast.render(graphics, graphics.guiWidth(), graphics.guiHeight());
    }
}
