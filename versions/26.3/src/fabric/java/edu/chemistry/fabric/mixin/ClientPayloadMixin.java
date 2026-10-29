package edu.chemistry.fabric.mixin;

import edu.chemistry.code.CodePayload;
import edu.chemistry.network.EduNetwork;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fabric: the mod's packets from the server go to the client's handler (Code Builder's go to the MakeCode editor). */
@Mixin(ClientPacketListener.class)
public abstract class ClientPayloadMixin {
    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void edu$payload(CustomPacketPayload payload, CallbackInfo ci) {
        if (payload instanceof CodePayload) {
            EduNetwork.clientHandler.accept(payload);
            ci.cancel();
        }
    }
}
