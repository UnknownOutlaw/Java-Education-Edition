package edu.chemistry.mixin;

import edu.chemistry.EduPermissions;
import edu.chemistry.EduRules;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Border columns near the player give off red particles drifting up and down, like Bedrock's border effect. */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
    @Unique
    private static final DustParticleOptions EDU_BORDER_DUST = new DustParticleOptions(0xFF2020, 1.0F);

    @Inject(method = "animateTick", at = @At("HEAD"))
    private void edu$borderParticles(int px, int py, int pz, CallbackInfo ci) {
        if (!EduRules.clientEnabled) return;
        ClientLevel level = (ClientLevel) (Object) this;
        RandomSource random = level.getRandom();
        for (int i = 0; i < 40; i++) {
            int x = px + random.nextInt(25) - 12;
            int z = pz + random.nextInt(25) - 12;
            if (!EduPermissions.isBorderColumn(level, x, z)) continue;
            for (int k = 0; k < 3; k++) {
                double y = py + random.nextDouble() * 16.0 - 8.0;
                double vy = random.nextBoolean() ? 0.08 : -0.08;
                level.addParticle(EDU_BORDER_DUST, x + random.nextDouble(), y, z + random.nextDouble(), 0.0, vy, 0.0);
            }
        }
    }
}
