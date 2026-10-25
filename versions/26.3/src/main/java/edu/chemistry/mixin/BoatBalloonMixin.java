package edu.chemistry.mixin;

import edu.chemistry.item.BalloonItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Bedrock lets you tie a balloon to a boat (mass 1, so it floats away with whoever is in it). */
@Mixin(AbstractBoat.class)
public abstract class BoatBalloonMixin {
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void edu$tieBalloon(Player player, InteractionHand hand, Vec3 location, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof BalloonItem balloon) {
            InteractionResult result = balloon.tryAttach(stack, player, (AbstractBoat) (Object) this);
            if (result != InteractionResult.PASS) {
                cir.setReturnValue(result);
            }
        }
    }
}
