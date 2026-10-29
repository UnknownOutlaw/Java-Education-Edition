package edu.chemistry.mixin;

import edu.chemistry.EduRules;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A new world starts with Education Edition off until its server says otherwise (the mod's sync packet, handled the
 * mod loader's way: on Fabric, through ClientPayloadMixin), which shows or hides the Chemistry creative tab to match.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleLogin", at = @At("HEAD"))
    private void edu$resetEducation(ClientboundLoginPacket packet, CallbackInfo ci) {
        EduRules.setClientEnabled(false);
        EduRules.clientImmutable = false;
        edu.chemistry.EduPermissions.clientWorldBuilder = false;
    }
}
