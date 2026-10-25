package edu.chemistry.mixin;

import edu.chemistry.client.legacy.LegacyWorldOptions;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Education Edition toggle in Legacy4J's (ReConsole's) world creation options: Game Options tab, right under
 * Host Privileges. Only applies when Legacy4J is installed, and a Legacy4J build that doesn't match is skipped instead
 * of stopping the game.
 */
@Pseudo
@Mixin(targets = "wily.legacy.client.screen.WorldMoreOptionsScreen")
public abstract class WorldMoreOptionsScreenMixin {
    /** the Host Privileges tick box, which only the new-world options make */
    @Unique
    private Object edu$hostPrivileges;
    @Unique
    private CreateWorldScreen edu$createWorld;

    @Inject(method = "createHostPrivilegesTickBox", at = @At("RETURN"), require = 0)
    private void edu$rememberHostPrivileges(CreateWorldScreen parent, CallbackInfoReturnable<?> cir) {
        this.edu$hostPrivileges = cir.getReturnValue();
        this.edu$createWorld = parent;
    }

    /** once the menu's lists are built (both constructors; only the new-world one has Host Privileges) */
    @Inject(method = "<init>*", at = @At("RETURN"), require = 0)
    private void edu$addEducationToggle(CallbackInfo ci) {
        if (this.edu$hostPrivileges != null) LegacyWorldOptions.addEducationToggle(this, this.edu$hostPrivileges, this.edu$createWorld);
    }
}
