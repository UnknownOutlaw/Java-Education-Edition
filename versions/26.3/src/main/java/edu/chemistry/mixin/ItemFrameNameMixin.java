package edu.chemistry.mixin;

import edu.chemistry.client.photo.PictureTaker;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.renderer.entity.ItemFrameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** named items in item frames keep their name tags in pictures too */
@Mixin(ItemFrameRenderer.class)
public abstract class ItemFrameNameMixin {
    @Redirect(method = "shouldShowName(Lnet/minecraft/world/entity/decoration/ItemFrame;D)Z",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Hud;isHidden()Z"))
    private boolean edu$namesInPictures(Hud hud) {
        return hud.isHidden() && !PictureTaker.hideThisFrame();
    }
}
