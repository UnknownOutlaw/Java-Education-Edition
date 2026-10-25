package edu.chemistry.mixin;

import edu.chemistry.util.BlockTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.PressurePlateBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Stone and blackstone pressure plates (the ones only mobs press) are pressed by the Agent and NPCs too. */
@Mixin(PressurePlateBlock.class)
public abstract class PressurePlateBlockMixin {
    @Inject(method = "getSignalStrength", at = @At("RETURN"), cancellable = true)
    private void edu$agentsAndNpcs(Level level, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        if (cir.getReturnValueI() == 0 && BlockTriggers.agentOrNpcOn(level, pos)) cir.setReturnValue(15);
    }
}
