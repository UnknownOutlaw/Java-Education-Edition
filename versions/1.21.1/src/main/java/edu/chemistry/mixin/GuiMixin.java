package edu.chemistry.mixin;

import edu.chemistry.client.photo.PhotoToast;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the control tips, and the camera's flash and the little picture that pops up after taking one. */
@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method = "render", at = @At("TAIL"))
    private void edu$photoToast(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        edu.chemistry.client.ControlTips.render(graphics, graphics.guiWidth(), graphics.guiHeight());
        PhotoToast.render(graphics, graphics.guiWidth(), graphics.guiHeight());
    }
}
