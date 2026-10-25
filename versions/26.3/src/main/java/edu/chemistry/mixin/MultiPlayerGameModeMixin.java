package edu.chemistry.mixin;

import edu.chemistry.EduRules;
import edu.chemistry.entity.BalloonEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Clicking a balloon's lead knot on a fence or wall: the click is still sent to the server (which unties the balloon),
 * but nothing else happens on this side - no block placed against the fence, no item used.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "performUseItemOn", at = @At("HEAD"), cancellable = true)
    private void edu$untieBalloon(LocalPlayer player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        Level level = Minecraft.getInstance().level;
        if (level == null || !EduRules.clientEnabled) return;
        BlockState state = level.getBlockState(hit.getBlockPos());
        if ((state.is(BlockTags.FENCES) || state.is(BlockTags.WALLS)) && BalloonEntity.untieFrom(level, hit.getBlockPos())) {
            cir.setReturnValue(InteractionResult.SUCCESS);
        }
    }
}
