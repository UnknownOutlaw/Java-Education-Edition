package edu.chemistry.item;

import edu.chemistry.data.ChemData;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

public class ElementBlockItem extends BlockItem {
    private final int atomicNumber;

    public ElementBlockItem(Block block, int atomicNumber, Properties properties) {
        super(block, properties);
        this.atomicNumber = atomicNumber;
    }

    public int atomicNumber() {
        return this.atomicNumber;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        if (this.atomicNumber == 0) {
            tooltip.add(Component.translatable("tooltip.edu.element.unknown").withStyle(ChatFormatting.GRAY));
            return;
        }
        tooltip.add(Component.translatable("tooltip.edu.element.symbol", ChemData.SYMBOLS[this.atomicNumber]).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.edu.element.number", this.atomicNumber).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.edu.element.mass", ChemData.MASS[this.atomicNumber]).withStyle(ChatFormatting.DARK_GRAY));
    }
}
