package edu.chemistry.fabric.mixin;

import edu.chemistry.code.CodeConnection;
import edu.chemistry.code.CodePayload;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Fabric: Code Builder packets from the player's MakeCode editor, handled on the server thread. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerPayloadMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void edu$codeBuilder(ServerboundCustomPayloadPacket packet, CallbackInfo ci) {
        if (!(packet.payload() instanceof CodePayload code)) return;
        ServerPlayer player = this.player;
        player.getServer().execute(() -> CodeConnection.receive(player, code.json()));
        ci.cancel();
    }
}
