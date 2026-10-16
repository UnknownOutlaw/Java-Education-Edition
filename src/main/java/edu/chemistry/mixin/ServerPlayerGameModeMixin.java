package edu.chemistry.mixin;

import edu.chemistry.EduRules;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** With Education Edition off, chemistry items can't be used or placed and the chemistry tables can't be opened. */
@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
    @Inject(method = "useItem", at = @At("HEAD"), cancellable = true)
    private void edu$blockUse(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        if (EduRules.isEdu(stack) && !EduRules.enabled(level)) {
            EduRules.tellDisabled(player);
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void edu$blockUseOn(ServerPlayer player, Level level, ItemStack stack, InteractionHand hand, BlockHitResult hit,
                                CallbackInfoReturnable<InteractionResult> cir) {
        if (EduRules.enabled(level)) {
            // clicking the lead knot on a fence or wall unties the balloon there
            net.minecraft.world.level.block.state.BlockState state = level.getBlockState(hit.getBlockPos());
            if ((state.is(net.minecraft.tags.BlockTags.FENCES) || state.is(net.minecraft.tags.BlockTags.WALLS))
                && edu.chemistry.entity.BalloonEntity.untieFrom(level, hit.getBlockPos())) {
                cir.setReturnValue(InteractionResult.SUCCESS);
            }
            return;
        }
        if (EduRules.isEdu(stack) || EduRules.isEdu(level.getBlockState(hit.getBlockPos()).getBlock())) {
            EduRules.tellDisabled(player);
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
