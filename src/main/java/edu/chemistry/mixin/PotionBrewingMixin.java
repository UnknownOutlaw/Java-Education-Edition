package edu.chemistry.mixin;

import edu.chemistry.registry.EduItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Bedrock's medicines are brewed in a brewing stand: an awkward potion plus silver (antidote), cobalt (elixir),
 * calcium (eye drops) or bismuth (tonic).
 */
@Mixin(PotionBrewing.class)
public abstract class PotionBrewingMixin {
    @Unique
    private static @Nullable Item edu$medicineFor(ItemStack ingredient) {
        if (ingredient.is(EduItems.ELEMENTS[47])) return EduItems.ANTIDOTE;
        if (ingredient.is(EduItems.ELEMENTS[27])) return EduItems.ELIXIR;
        if (ingredient.is(EduItems.ELEMENTS[20])) return EduItems.EYE_DROPS;
        if (ingredient.is(EduItems.ELEMENTS[83])) return EduItems.TONIC;
        return null;
    }

    @Unique
    private static boolean edu$isAwkward(ItemStack stack) {
        if (!stack.is(Items.POTION)) return false;
        PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
        return contents != null && contents.is(Potions.AWKWARD);
    }

    @Inject(method = "isIngredient", at = @At("HEAD"), cancellable = true)
    private void edu$isIngredient(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (edu$medicineFor(stack) != null) cir.setReturnValue(true);
    }

    @Inject(method = "hasMix", at = @At("HEAD"), cancellable = true)
    private void edu$hasMix(ItemStack input, ItemStack ingredient, CallbackInfoReturnable<Boolean> cir) {
        if (edu$medicineFor(ingredient) != null) cir.setReturnValue(edu$isAwkward(input));
    }

    @Inject(method = "mix", at = @At("HEAD"), cancellable = true)
    private void edu$mix(ItemStack ingredient, ItemStack input, CallbackInfoReturnable<ItemStack> cir) {
        Item medicine = edu$medicineFor(ingredient);
        if (medicine != null) {
            cir.setReturnValue(edu$isAwkward(input) ? new ItemStack(medicine) : input);
        }
    }
}
