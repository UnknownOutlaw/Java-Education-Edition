package edu.chemistry.mixin;

import edu.chemistry.client.ControlTips;
import java.util.Arrays;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds the "Control Tips" on/off switch to Options → Controls. */
@Mixin(ControlsScreen.class)
public abstract class ControlsScreenMixin {
    @Inject(method = "options", at = @At("RETURN"), cancellable = true)
    private static void edu$controlTipsOption(Options options, CallbackInfoReturnable<OptionInstance<?>[]> cir) {
        ControlTips.ensureLoaded();
        OptionInstance<?>[] list = cir.getReturnValue();
        OptionInstance<?>[] more = Arrays.copyOf(list, list.length + 1);
        more[list.length] = ControlTips.OPTION;
        cir.setReturnValue(more);
    }
}
