package edu.chemistry.mixin;

import edu.chemistry.client.photo.PictureTaker;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** the HUD is hidden for the picture, but everyone's name tags still show in it */
@Mixin(LivingEntityRenderer.class)
public abstract class NameTagMixin {
    @Redirect(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;isHidden()Z"))
    private boolean edu$namesInPictures(Hud hud) {
        return hud.isHidden() && !PictureTaker.hideThisFrame();
    }
}
