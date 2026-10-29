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

/**
 * Adds the control tips key (H) and the Code Builder key (C) to the game's key bindings before they're loaded, so they
 * can be rebound and are saved. Done when the options are first loaded (in their constructor): Forge's Mixin can't hook
 * into the middle of a constructor.
 */
@Mixin(Options.class)
public abstract class OptionsMixin {
    @Shadow
    @Final
    @Mutable
    public KeyMapping[] keyMappings;

    @Inject(method = "load()V", at = @At("HEAD"))
    private void edu$addControlTipsKey(CallbackInfo ci) {
        if (Arrays.asList(this.keyMappings).contains(ControlTips.TOGGLE_KEY)) return;
        KeyMapping[] more = Arrays.copyOf(this.keyMappings, this.keyMappings.length + 2);
        more[this.keyMappings.length] = ControlTips.TOGGLE_KEY;
        more[this.keyMappings.length + 1] = edu.chemistry.client.code.CodeBuilderClient.KEY;
        this.keyMappings = more;
    }
}
