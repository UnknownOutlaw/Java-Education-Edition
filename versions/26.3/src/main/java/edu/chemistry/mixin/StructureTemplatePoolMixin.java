package edu.chemistry.mixin;

import edu.chemistry.world.EduVillages;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.structure.pools.EmptyPoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Villages only build the Chemist's lab while Education Edition is on: otherwise the pools leave it out of their picks. */
@Mixin(StructureTemplatePool.class)
public abstract class StructureTemplatePoolMixin {
    @Shadow
    @Final
    private ObjectArrayList<StructurePoolElement> templates;

    @Inject(method = "getShuffledTemplates", at = @At("RETURN"), cancellable = true)
    private void edu$noLabsWhenOff(RandomSource random, CallbackInfoReturnable<List<StructurePoolElement>> cir) {
        if (EduVillages.containsLab(cir.getReturnValue()) && !EduVillages.labsAllowed()) {
            cir.setReturnValue(EduVillages.withoutLabs(cir.getReturnValue()));
        }
    }

    @Inject(method = "getRandomTemplate", at = @At("HEAD"), cancellable = true)
    private void edu$noRandomLabWhenOff(RandomSource random, CallbackInfoReturnable<StructurePoolElement> cir) {
        if (!EduVillages.containsLab(this.templates) || EduVillages.labsAllowed()) return;
        List<StructurePoolElement> rest = EduVillages.withoutLabs(this.templates);
        cir.setReturnValue(rest.isEmpty() ? EmptyPoolElement.INSTANCE : rest.get(random.nextInt(rest.size())));
    }
}
