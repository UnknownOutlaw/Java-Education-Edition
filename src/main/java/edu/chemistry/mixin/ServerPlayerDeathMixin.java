package edu.chemistry.mixin;

import edu.chemistry.code.CodeEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tells Code Builder when a player dies (MakeCode's "on player died"). */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerDeathMixin {
    @Inject(method = "die", at = @At("HEAD"))
    private void edu$died(DamageSource source, CallbackInfo ci) {
        CodeEvents.died((ServerPlayer) (Object) this);
    }
}
