package edu.chemistry.item;

import edu.chemistry.EduRules;
import edu.chemistry.agent.Agents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Education's Agent Spawn Egg: puts your Agent where you click, facing the way you face. Each player has one Agent, so
 * if you already have one it comes to that spot instead of a second one appearing.
 */
public class AgentSpawnEggItem extends Item {
    public AgentSpawnEggItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(context.getPlayer() instanceof ServerPlayer player) || !(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        if (!EduRules.enabled(level)) {
            EduRules.tellDisabled(player);
            return InteractionResult.FAIL;
        }
        BlockPos clicked = context.getClickedPos();
        BlockPos pos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty() ? clicked : clicked.relative(context.getClickedFace());
        // like placing an armor stand, it turns to face whoever placed it
        double dx = player.getX() - (pos.getX() + 0.5);
        double dz = player.getZ() - (pos.getZ() + 0.5);
        net.minecraft.core.Direction facing = dx * dx + dz * dz < 0.01 ? player.getDirection().getOpposite()
            : net.minecraft.core.Direction.fromYRot(net.minecraft.util.Mth.atan2(dz, dx) * net.minecraft.util.Mth.RAD_TO_DEG - 90.0);
        if (!Agents.summon(player, server, Vec3.atBottomCenterOf(pos), facing)) return InteractionResult.FAIL;
        context.getItemInHand().consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
