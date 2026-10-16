package edu.chemistry.mixin;

import edu.chemistry.block.ChalkboardEdit;
import java.util.List;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.FilteredText;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Chalkboard edits travel in the sign-update packet (see {@link ChalkboardEdit}); this hands them to the chalkboard. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerMixin {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "updateSignText", at = @At("HEAD"), cancellable = true)
    private void edu$chalkboard(ServerboundSignUpdatePacket packet, List<FilteredText> lines, CallbackInfo ci) {
        List<String> raw = lines.stream().map(FilteredText::raw).toList();
        if (ChalkboardEdit.handle(this.player, packet.getPos(), raw) || edu.chemistry.entity.NpcNetwork.handle(this.player, packet.getPos(), raw)
            || edu.chemistry.photo.PhotoNetwork.handle(this.player, packet.getPos(), raw)) {
            ci.cancel();
        }
    }

    /** players who joined the world can't put World Builder commands in command blocks (or command block minecarts) */
    @Inject(method = "handleSetCommandBlock", at = @At("HEAD"), cancellable = true)
    private void edu$commandBlock(net.minecraft.network.protocol.game.ServerboundSetCommandBlockPacket packet, CallbackInfo ci) {
        if (edu.chemistry.EduPermissions.isWorldBuilderCommand(packet.getCommand()) && !edu.chemistry.EduPermissions.isHost(this.player)) {
            edu.chemistry.EduPermissions.tellHostOnly(this.player);
            ci.cancel();
        }
    }

    @Inject(method = "handleSetCommandMinecart", at = @At("HEAD"), cancellable = true)
    private void edu$commandMinecart(net.minecraft.network.protocol.game.ServerboundSetCommandMinecartPacket packet, CallbackInfo ci) {
        if (edu.chemistry.EduPermissions.isWorldBuilderCommand(packet.getCommand()) && !edu.chemistry.EduPermissions.isHost(this.player)) {
            edu.chemistry.EduPermissions.tellHostOnly(this.player);
            ci.cancel();
        }
    }
}
