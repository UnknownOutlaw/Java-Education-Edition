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
 * Education's Agent Spawn Egg: puts your Agent where you click, facing away from you. Each player has one Agent, so
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
        // it faces away from whoever placed it: along the line from the player to the spot (the player's own way when
        // placed right where the player stands)
        double dx = (pos.getX() + 0.5) - player.getX();
        double dz = (pos.getZ() + 0.5) - player.getZ();
        net.minecraft.core.Direction facing = dx * dx + dz * dz < 0.01 ? player.getDirection()
            : net.minecraft.core.Direction.fromYRot(net.minecraft.util.Mth.atan2(dz, dx) * net.minecraft.util.Mth.RAD_TO_DEG - 90.0);
        if (!Agents.summon(player, server, Vec3.atBottomCenterOf(pos), facing)) return InteractionResult.FAIL;
        context.getItemInHand().consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
