package edu.chemistry.item;

import edu.chemistry.EduRules;
import edu.chemistry.photo.PhotoHooks;
import edu.chemistry.photo.Photos;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/** A picture saved out of a portfolio. Use it to look at it; add it to a portfolio with the portfolio's Add Photo. */
public class PhotoItem extends Item {
    public PhotoItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() && EduRules.clientEnabled && Photos.image(stack).isPresent()) PhotoHooks.viewPhoto.accept(stack);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        String caption = Photos.caption(stack);
        if (!caption.isBlank()) tooltip.accept(Component.literal(caption).withStyle(ChatFormatting.GRAY));
    }
}
