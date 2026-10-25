package edu.chemistry.mixin;

import edu.chemistry.code.CodeEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tells Code Builder when a player kills a mob (MakeCode's "on mob killed"). Players' own deaths: ServerPlayerDeathMixin. */
@Mixin(LivingEntity.class)
public abstract class LivingEntityDeathMixin {
    @Inject(method = "die", at = @At("HEAD"))
    private void edu$died(DamageSource source, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (self.level().isClientSide() || self.isRemoved()) return;
        if (!(self instanceof ServerPlayer) && source.getEntity() instanceof ServerPlayer killer) CodeEvents.mobKilled(killer, self);
    }
}
