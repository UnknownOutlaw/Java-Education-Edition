package edu.chemistry.item;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResultHolder;
import edu.chemistry.EduRules;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** The Chemistry Guide: a book with every chemistry recipe in it. */
public class GuideBookItem extends Item {
    /** opens the book (set by the client) */
    public static Runnable open = () -> {
    };

    public GuideBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() && EduRules.clientEnabled) open.run();
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
}
