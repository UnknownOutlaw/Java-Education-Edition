package edu.chemistry.mixin;

import edu.chemistry.EduRules;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRule;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the Chemist's lab to villages, and tells every client when Education Edition is switched on or off, so the Chemistry tab appears or disappears. */
@Mixin(MinecraftServer.class)
public abstract class MinecraftServerMixin {
    /** new villages can build the Chemist's lab: add it to the house pools as soon as the world's registries exist */
    @Inject(method = "<init>", at = @At("TAIL"))
    private void edu$addChemistLabs(CallbackInfo ci) {
        edu.chemistry.world.EduVillages.addHouses((MinecraftServer) (Object) this);
        edu.chemistry.agent.Agents.reset();
        edu.chemistry.code.CodeConnection.reset();
    }

    /** Code Builder: answers that wait for the Agent's step, and movement events */
    @Inject(method = "tickServer", at = @At("TAIL"))
    private void edu$codeBuilderTick(java.util.function.BooleanSupplier hasTime, CallbackInfo ci) {
        edu.chemistry.code.CodeConnection.tick((MinecraftServer) (Object) this);
    }

    @Inject(method = "onGameRuleChanged", at = @At("TAIL"))
    private <T> void edu$syncEducationRule(GameRule<T> rule, T value, CallbackInfo ci) {
        if (rule == EduRules.EDUCATION_FEATURES) {
            ((MinecraftServer) (Object) this).getPlayerList().broadcastAll(EduRules.syncPacket(Boolean.TRUE.equals(value)));
        }
        if (rule == EduRules.IMMUTABLE_WORLD) {
            ((MinecraftServer) (Object) this).getPlayerList().broadcastAll(EduRules.syncPacket("immutable_world", Boolean.TRUE.equals(value)));
        }
    }
}
