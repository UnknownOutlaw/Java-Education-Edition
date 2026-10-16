package edu.chemistry.mixin;

import edu.chemistry.client.ControlTips;
import java.util.Arrays;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the control tips key (H) to the game's key bindings before they're loaded, so it can be rebound and is saved. */
@Mixin(Options.class)
public abstract class OptionsMixin {
    @Shadow
    @Final
    @Mutable
    public KeyMapping[] keyMappings;

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;load()V"))
    private void edu$addControlTipsKey(CallbackInfo ci) {
        KeyMapping[] more = Arrays.copyOf(this.keyMappings, this.keyMappings.length + 1);
        more[this.keyMappings.length] = ControlTips.TOGGLE_KEY;
        this.keyMappings = more;
    }
}
