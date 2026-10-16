package edu.chemistry.mixin;

import edu.chemistry.entity.TripodCameraEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Vanilla never draws your own player unless the view is your own (so you don't see yourself while spectating). When
 * the view is a tripod camera's lens - its picture of you - your player is drawn, like security-camera mods do.
 */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
    @Redirect(method = "extractVisibleEntities", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/Camera;entity()Lnet/minecraft/world/entity/Entity;", ordinal = 3))
    private Entity edu$showPlayerToTripod(Camera camera) {
        Entity viewer = camera.entity();
        if (viewer instanceof TripodCameraEntity && Minecraft.getInstance().player != null) return Minecraft.getInstance().player;
        return viewer;
    }
}
