package edu.chemistry.client;

import edu.chemistry.EduMod;
import edu.chemistry.data.ChemData;
import edu.chemistry.item.CompoundItem;
import edu.chemistry.menu.CompoundCreatorMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Bedrock layout: centred title, grid on the compound background, connector, output with its formula underneath. */
public class CompoundCreatorScreen extends EduContainerScreen<CompoundCreatorMenu> {
    public CompoundCreatorScreen(CompoundCreatorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, EduMod.id("textures/gui/container/compound_creator.png"), 184, 167);
        this.inventoryLabelX = 12;
        // title 1px higher than the vanilla default (6)
        this.titleLabelY = 5;
    }

    private final StationRecipeBook book = new StationRecipeBook(edu.chemistry.registry.EduRecipes.COMPOUND);

    @Override
    protected StationRecipeBook recipeBook() {
        return this.book;
    }

    /** left of the element grid */
    @Override
    protected int[] recipeButtonPos() {
        return new int[] {7, 20};
    }

    @Override
    protected java.util.List<net.minecraft.world.inventory.Slot> gridSlots() {
        return this.menu.slots.subList(1, 10);
    }

    @Override
    protected net.minecraft.world.inventory.Slot resultSlot() {
        return this.menu.getSlot(0);
    }

    static String formula(ItemStack stack) {
        if (stack.getItem() instanceof CompoundItem c) {
            for (int i = 0; i < ChemData.COMPOUND_IDS.length; i++) {
                if (ChemData.COMPOUND_IDS[i].equals(c.compoundId())) return ChemData.COMPOUND_FORMULAS[i];
            }
        }
        if (stack.is(Items.CHARCOAL)) return "C₇H₄O";
        if (stack.is(Items.INK_SAC)) return "FeSO₄";
        if (stack.is(Items.SUGAR)) return "C₆H₁₂O₆";
        return "";
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        this.drawTitle(graphics);
        // Bedrock's compound creator has no "Inventory" label
        ItemStack result = this.menu.getSlot(0).getItem();
        if (!result.isEmpty()) {
            String f = formula(result);
            if (!f.isEmpty()) {
                int w = this.font.width(f);
                graphics.text(this.font, f, Math.min(this.imageWidth - 4 - w, 146 - w / 2), 66, TITLE_COLOR, false);
            }
        }
    }
}
