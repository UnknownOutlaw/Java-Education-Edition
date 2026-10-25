package edu.chemistry.mixin;

import edu.chemistry.EduPermissions;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Border blocks make an invisible wall that goes all the way up and down their column: players (except World Builders)
 * and mobs (except the ender dragon) can't walk, jump, climb, swim or tunnel through, over or under it.
 */
@Mixin(Entity.class)
public abstract class EntityBorderMixin {
    @Inject(method = "collectColliders", at = @At("RETURN"), cancellable = true)
    private static void edu$borderWall(Entity entity, Level level, List<VoxelShape> entityShapes, AABB box,
                                       CallbackInfoReturnable<List<VoxelShape>> cir) {
        if (!(entity instanceof LivingEntity) || entity instanceof EnderDragon || level == null) return;
        if (entity instanceof Player player && EduPermissions.isWorldBuilder(player)) return;
        if (!edu.chemistry.EduRules.enabled(level) || (level.isClientSide() && !edu.chemistry.EduRules.clientEnabled)) return;
        AABB current = entity.getBoundingBox();
        List<VoxelShape> extra = null;
        int x0 = (int) Math.floor(box.minX);
        int x1 = (int) Math.floor(box.maxX - 1.0E-7);
        int z0 = (int) Math.floor(box.minZ);
        int z1 = (int) Math.floor(box.maxZ - 1.0E-7);
        if ((x1 - x0 + 1) * (z1 - z0 + 1) > 64) return; // not for huge sweeps
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                // never trap something that is already standing in the column
                if (current.maxX > x && current.minX < x + 1 && current.maxZ > z && current.minZ < z + 1) continue;
                if (!EduPermissions.isBorderColumn(level, x, z)) continue;
                if (extra == null) extra = new ArrayList<>(cir.getReturnValue());
                extra.add(Shapes.create(new AABB(x, box.minY - 2.0, z, x + 1, box.maxY + 2.0, z + 1)));
            }
        }
        if (extra != null) cir.setReturnValue(extra);
    }
}
