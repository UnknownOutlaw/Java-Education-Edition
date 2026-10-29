package edu.chemistry.fabric.mixin;

import edu.chemistry.network.EduSyncPayload;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets the game's custom packet codec read and write the mod's packets: the sync packet and Code Builder's (both sides use the same codec). */
@Mixin(targets = "net.minecraft.network.protocol.common.custom.CustomPacketPayload$1")
public abstract class CustomPayloadCodecMixin {
    @Inject(method = "findCodec", at = @At("HEAD"), cancellable = true)
    private void edu$syncCodec(ResourceLocation id, CallbackInfoReturnable<Object> cir) {
        if (EduSyncPayload.TYPE.id().equals(id)) cir.setReturnValue(EduSyncPayload.CODEC);
        if (edu.chemistry.code.CodePayload.TYPE.id().equals(id)) cir.setReturnValue(edu.chemistry.code.CodePayload.CODEC);
    }
}
