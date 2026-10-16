package edu.chemistry.mixin;

import edu.chemistry.EduPermissions;
import edu.chemistry.block.ChalkboardBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Breaking blocks under a Deny, in a Border column, or breaking permission blocks, needs World Builder (client and server). */
@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "blockActionRestricted", at = @At("HEAD"), cancellable = true)
    private void edu$checkPermission(Level level, BlockPos pos, GameType gameType, CallbackInfoReturnable<Boolean> cir) {
        Player player = (Player) (Object) this;
        if (!EduPermissions.canBuild(level, player, pos)) {
            EduPermissions.tellCantBuild(player);
            cir.setReturnValue(true);
            return;
        }
        // also stops creative players' instant break, which skips destroy progress
        if (ChalkboardBlock.protectedFrom(level, pos, player)) {
            player.sendOverlayMessage(Component.translatable("message.edu.chalkboard_protected").withStyle(ChatFormatting.RED));
            cir.setReturnValue(true);
        }
    }
}
