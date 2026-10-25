package edu.chemistry.mixin;

import edu.chemistry.EduMod;
import edu.chemistry.EduRules;
import net.minecraft.client.multiplayer.ClientPacketListener;
import edu.chemistry.network.EduSyncPayload;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Picks up the Education Edition rule from the server and shows or hides the Chemistry creative tab to match. */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "handleLogin", at = @At("HEAD"))
    private void edu$resetEducation(ClientboundLoginPacket packet, CallbackInfo ci) {
        edu$setEducation(false);
        EduRules.clientImmutable = false;
        edu.chemistry.EduPermissions.clientWorldBuilder = false;
    }

    @Inject(method = "handleCustomPayload", at = @At("HEAD"), cancellable = true)
    private void edu$readEducation(CustomPacketPayload payload, CallbackInfo ci) {
        // Code Builder packets from the server go to the MakeCode editor
        if (payload instanceof edu.chemistry.code.CodePayload code) {
            edu.chemistry.client.code.CodeBuilderClient.fromServer(code.json());
            ci.cancel();
            return;
        }
        if (!(payload instanceof EduSyncPayload sync)) return;
        switch (sync.name()) {
            case "education_features" -> edu$setEducation(sync.value());
            case "immutable_world" -> EduRules.clientImmutable = sync.value();
            case "world_builder" -> edu.chemistry.EduPermissions.clientWorldBuilder = sync.value();
            default -> {
            }
        }
        ci.cancel();
    }

    @Unique
    private static void edu$setEducation(boolean enabled) {
        if (EduRules.clientEnabled == enabled) return;
        EduRules.clientEnabled = enabled;
        EduRules.onClientEnabledChanged.run();
        // make the creative screen rebuild its tabs (and search) next time it looks
        edu.chemistry.registry.EduTabs.refresh();
    }
}
