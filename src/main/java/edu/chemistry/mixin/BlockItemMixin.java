package edu.chemistry.mixin;

import edu.chemistry.EduPermissions;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Allow / Deny / Border: placing blocks where you aren't allowed to build, and placing permission blocks at all, needs World Builder. */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
    @Inject(method = "place", at = @At("HEAD"), cancellable = true)
    private void edu$checkPermission(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Player player = context.getPlayer();
        if (player == null) return;
        boolean permissionBlock = EduPermissions.isPermissionBlock(((BlockItem) (Object) this).getBlock());
        boolean allowed = permissionBlock
            ? EduPermissions.isWorldBuilder(player) || !edu.chemistry.EduRules.enabled(context.getLevel())
            : EduPermissions.canBuild(context.getLevel(), player, context.getClickedPos());
        if (!allowed) {
            EduPermissions.tellCantBuild(player);
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}
