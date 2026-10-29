package edu.chemistry.fabric.mixin;

import edu.chemistry.code.CodePayload;
import edu.chemistry.network.EduNetwork;
import edu.chemistry.network.EduSyncPayload;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Fabric: the mod's packets from the server go to the client's handler (Code Builder's to the MakeCode editor, the sync
 * packet to the rules it carries).
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPayloadMixin {
    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void edu$payload(CustomPacketPayload payload, CallbackInfo ci) {
        if (payload instanceof CodePayload || payload instanceof EduSyncPayload) {
            EduNetwork.clientHandler.accept(payload);
            ci.cancel();
        }
    }
}
