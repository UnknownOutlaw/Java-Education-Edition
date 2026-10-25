package edu.chemistry.mixin;

import edu.chemistry.client.EduClient;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.color.item.ItemColors;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 1.21.1 tints items in code, not in item model files: balloons take their dye colour this way. */
@Mixin(ItemColors.class)
public abstract class ItemColorsMixin {
    @Inject(method = "createDefault", at = @At("RETURN"))
    private static void edu$tints(BlockColors blockColors, CallbackInfoReturnable<ItemColors> cir) {
        EduClient.registerItemColors(cir.getReturnValue());
    }
}
