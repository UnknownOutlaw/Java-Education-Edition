package edu.chemistry.mixin;

import edu.chemistry.entity.BalloonEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.decoration.LeashFenceKnotEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Clicking a lead knot on a fence unties any balloons tied there (they float away); other mobs work as in vanilla. */
@Mixin(LeashFenceKnotEntity.class)
public abstract class LeashFenceKnotMixin {
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void edu$untieBalloons(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
        LeashFenceKnotEntity self = (LeashFenceKnotEntity) (Object) this;
        if (self.level().isClientSide()) return;
        // a player bringing their own leashed mobs to the knot ties them up, as in vanilla
        if (!Leashable.leashableLeashedTo(player).isEmpty()) return;
        boolean any = false;
        for (Leashable leashable : Leashable.leashableLeashedTo(self)) {
            if (leashable instanceof BalloonEntity balloon) {
                balloon.untie();
                any = true;
            }
        }
        // nothing else on this knot: it has gone (vanilla removes an empty knot) and the click is done
        if (any && Leashable.leashableLeashedTo(self).isEmpty()) cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
