package edu.chemistry.mixin;

import edu.chemistry.client.legacy.LegacySettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Control Tips toggle in Legacy4J's (ReConsole's) Key Binds menu, right after its own toggles (Toggle Crouch and
 * the rest). Only applies when Legacy4J is installed, and a Legacy4J build that doesn't match is skipped instead of
 * stopping the game.
 */
@Pseudo
@Mixin(targets = "wily.legacy.client.screen.LegacyKeyMappingScreen")
public abstract class LegacyKeyMappingScreenMixin {
    @Inject(method = "addButtons", require = 0, at = @At(value = "INVOKE", shift = At.Shift.AFTER, ordinal = 0,
        target = "Lwily/legacy/client/screen/RenderableVList;addOptions([Lwily/factoryapi/base/config/FactoryConfig;)Lwily/legacy/client/screen/RenderableVList;"))
    private void edu$controlTipsToggle(CallbackInfo ci) {
        LegacySettings.addToKeyBinds(this);
    }
}
