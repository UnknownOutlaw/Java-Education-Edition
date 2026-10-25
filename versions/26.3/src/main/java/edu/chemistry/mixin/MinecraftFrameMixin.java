package edu.chemistry.mixin;

import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.device.GpuSurface;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import edu.chemistry.client.photo.PictureTaker;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The picture's own frames (looking through a tripod camera, with the HUD hidden) are never shown on the screen: while
 * they're drawn, the window keeps showing a copy of the last normal frame.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftFrameMixin {
    @Redirect(method = "renderFrame", at = @At(value = "INVOKE",
        target = "Lcom/mojang/renderpearl/api/device/GpuSurface;blitFromTexture(Lcom/mojang/renderpearl/api/commands/CommandEncoder;Lcom/mojang/renderpearl/api/textures/GpuTextureView;)V"))
    private void edu$keepPictureFramesOffScreen(GpuSurface surface, CommandEncoder encoder, GpuTextureView texture) {
        surface.blitFromTexture(encoder, PictureTaker.shownFrame(texture));
    }

    /** Code Builder's key, and telling the MakeCode editor when a world is joined or left */
    @Inject(method = "tick", at = @At("TAIL"))
    private void edu$codeBuilder(CallbackInfo ci) {
        edu.chemistry.client.code.CodeBuilderClient.tick();
    }
}
