package edu.chemistry.item;

import edu.chemistry.EduPermissions;
import edu.chemistry.entity.NpcEntity;
import edu.chemistry.registry.EduEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Education's NPC spawn egg; like Education, only World Builders can place NPCs. */
public class NpcSpawnEggItem extends Item {
    public NpcSpawnEggItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player == null || !EduPermissions.isWorldBuilder(player)) {
            if (player instanceof ServerPlayer sp) {
                sp.displayClientMessage(Component.translatable("message.edu.npc_needs_wb").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.FAIL;
        }
        if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
        BlockPos clicked = context.getClickedPos();
        BlockPos pos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty() ? clicked : clicked.relative(context.getClickedFace());
        NpcEntity npc = new NpcEntity(EduEntities.NPC, level);
        npc.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot() + 180.0F, 0.0F);
        npc.setYHeadRot(player.getYRot() + 180.0F);
        npc.setCustomName(Component.literal("NPC"));
        npc.setCustomNameVisible(true);
        java.util.List<String> skins = NpcEntity.skinsInPack(0);
        npc.setSkin(skins.get(level.getRandom().nextInt(skins.size())));
        server.addFreshEntity(npc);
        context.getItemInHand().consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
