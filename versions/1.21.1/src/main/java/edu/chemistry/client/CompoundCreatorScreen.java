package edu.chemistry.client;

import edu.chemistry.EduMod;
import edu.chemistry.data.ChemData;
import edu.chemistry.item.CompoundItem;
import edu.chemistry.menu.CompoundCreatorMenu;
import net.minecraft.client.gui.GuiGraphics;
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

    /**
     * Console layout: the 3x3 element grid, an arrow, and the compound in a big result frame like the furnace's, centred
     * across the panel. (The recipe book is hidden with Legacy4J.)
     */
    public static edu.chemistry.client.legacy.StationLayout layout(boolean sd) {
        var l = new edu.chemistry.client.legacy.StationLayout(sd);
        int g = l.slot;
        int gridX = (l.width - (3 * g + l.s(10) + l.arrowWidth() + l.s(10) + l.resultSize())) / 2;
        int gridY = l.s(26);
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) l.put(1 + c + r * 3, gridX + c * g, gridY + r * g, g);
        }
        int[] arrow = l.part("arrow", gridX + 3 * g + l.s(10), gridY + (3 * g - l.arrowHeight()) / 2, l.arrowWidth(), l.arrowHeight());
        int res = l.resultSize();
        int resX = arrow[0] + arrow[2] + l.s(10);
        int resY = gridY + (3 * g - res) / 2;
        l.put(0, resX, resY, res);
        l.part("formula", resX + res / 2, resY + res + l.s(4), 0, 0);
        l.inventory(10, l.titleX, gridY + 3 * g + l.s(12));
        return l;
    }

    @Override
    protected edu.chemistry.client.legacy.StationLayout legacyLayout(boolean sd) {
        return layout(sd);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float a, int mouseX, int mouseY) {
        super.renderBg(graphics, a, mouseX, mouseY);
        if (this.legacy != null) {
            int[] p = this.legacy.part("arrow");
            edu.chemistry.client.legacy.LegacyUi.arrow(graphics, this.leftPos + p[0], this.topPos + p[1], this.legacy.sd);
        }
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
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        this.labels(() -> {
            this.drawTitle(graphics);
            // Bedrock's compound creator has no "Inventory" label; the console layout has one like every console menu
            if (this.legacy != null) {
                graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, this.labelColor(), false);
            }
            ItemStack result = this.menu.getSlot(0).getItem();
            if (!result.isEmpty()) {
                String f = formula(result);
                if (!f.isEmpty()) {
                    int w = this.font.width(f);
                    if (this.legacy != null) {
                        int[] p = this.legacy.part("formula");
                        graphics.drawString(this.font, f, Math.max(2, Math.min(this.imageWidth - 4 - w, p[0] - w / 2)), p[1], this.labelColor(), false);
                    } else {
                        graphics.drawString(this.font, f, Math.min(this.imageWidth - 4 - w, 146 - w / 2), 66, TITLE_COLOR, false);
                    }
                }
            }
        });
    }
}
