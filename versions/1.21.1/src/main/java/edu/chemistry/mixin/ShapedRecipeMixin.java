package edu.chemistry.mixin;

import edu.chemistry.EduRules;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** With Education Edition off, recipes that make chemistry items don't craft. */
@Mixin(ShapedRecipe.class)
public abstract class ShapedRecipeMixin {
    @Shadow @Final ItemStack result;

    @Inject(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z", at = @At("HEAD"),
        cancellable = true)
    private void edu$toggle(CraftingInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (EduRules.isEdu(this.result.getItem()) && !EduRules.enabled(level)) cir.setReturnValue(false);
    }
}
