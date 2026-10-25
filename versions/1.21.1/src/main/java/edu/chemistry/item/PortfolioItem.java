package edu.chemistry.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResultHolder;
import edu.chemistry.EduRules;
import edu.chemistry.photo.PhotoHooks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Education's portfolio: opens the book of pictures you've taken. */
public class PortfolioItem extends Item {
    public PortfolioItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() && EduRules.clientEnabled) PhotoHooks.openPortfolio.run();
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
