package edu.chemistry.item;

import edu.chemistry.data.ChemData;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class CompoundItem extends Item {
    private final int index;

    public CompoundItem(int index, Properties properties) {
        super(properties);
        this.index = index;
    }

    public String compoundId() {
        return ChemData.COMPOUND_IDS[this.index];
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> tooltip, TooltipFlag flag) {
        String formula = ChemData.COMPOUND_FORMULAS[this.index];
        if (!formula.isEmpty()) {
            tooltip.add(Component.literal(formula).withStyle(ChatFormatting.GRAY));
        }
    }
}
