package edu.chemistry.mixin;

import edu.chemistry.EduMod;
import edu.chemistry.EduRules;
import java.lang.reflect.Field;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundGameRuleValuesPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.world.item.CreativeModeTabs;
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

    @Inject(method = "handleGameRuleValues", at = @At("TAIL"))
    private void edu$readEducation(ClientboundGameRuleValuesPacket packet, CallbackInfo ci) {
        packet.values().forEach((key, value) -> {
            if (key.identifier().equals(EduMod.id("education_features"))) edu$setEducation(Boolean.parseBoolean(value));
            if (key.identifier().equals(EduMod.id("immutable_world"))) EduRules.clientImmutable = Boolean.parseBoolean(value);
            if (key.identifier().equals(EduMod.id("world_builder"))) edu.chemistry.EduPermissions.clientWorldBuilder = Boolean.parseBoolean(value);
        });
    }

    @Unique
    private static void edu$setEducation(boolean enabled) {
        if (EduRules.clientEnabled == enabled) return;
        EduRules.clientEnabled = enabled;
        EduRules.onClientEnabledChanged.run();
        // make the creative screen rebuild its tabs (and search) next time it looks
        try {
            Field cached = CreativeModeTabs.class.getDeclaredField("CACHED_PARAMETERS");
            cached.setAccessible(true);
            cached.set(null, null);
        } catch (ReflectiveOperationException e) {
            EduMod.LOG.warn("Couldn't refresh the creative tabs", e);
        }
    }
}
