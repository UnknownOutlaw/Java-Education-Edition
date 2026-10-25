package edu.chemistry.mixin;

import edu.chemistry.EduRules;
import net.minecraft.network.Connection;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sends a joining player whether Education Edition is on in this world, and wakes up the player's Agent. */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Shadow @Final private MinecraftServer server;

    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void edu$sendEducationRule(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        player.connection.send(EduRules.syncPacket(this.server.getGameRules().getBoolean(EduRules.EDUCATION_FEATURES)));
        player.connection.send(EduRules.syncPacket("immutable_world", this.server.getGameRules().getBoolean(EduRules.IMMUTABLE_WORLD)));
        player.connection.send(EduRules.syncPacket("world_builder", player.getTags().contains(edu.chemistry.EduPermissions.WORLD_BUILDER_TAG)));
        edu.chemistry.agent.Agents.onJoin(player);
    }
}
