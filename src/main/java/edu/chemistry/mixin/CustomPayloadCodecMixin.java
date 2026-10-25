package edu.chemistry.mixin;

import edu.chemistry.code.CodePayload;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Lets the game's custom packet codec read and write the mod's Code Builder packet (both sides use the same codec). */
@Mixin(targets = "net.minecraft.network.protocol.common.custom.CustomPacketPayload$1")
public abstract class CustomPayloadCodecMixin {
    @Inject(method = "findCodec", at = @At("HEAD"), cancellable = true)
    private void edu$codeCodec(Identifier id, CallbackInfoReturnable<Object> cir) {
        if (CodePayload.TYPE.id().equals(id)) cir.setReturnValue(CodePayload.CODEC);
    }
}
