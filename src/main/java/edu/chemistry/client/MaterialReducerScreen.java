package edu.chemistry.client;

import edu.chemistry.EduMod;
import edu.chemistry.menu.MaterialReducerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Bedrock layout: input on top, connector lines fanning out to 5 + 4 output slots; input turns red while locked. */
public class MaterialReducerScreen extends EduContainerScreen<MaterialReducerMenu> {
    public MaterialReducerScreen(MaterialReducerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, EduMod.id("textures/gui/container/material_reducer.png"), 205, 200);
        this.inventoryLabelX = MaterialReducerMenu.INV_X;
        this.inventoryLabelY = MaterialReducerMenu.INV_Y - 11;
    }

    /**
     * Console layout: the item to break down at the top, joined by lines to its nine element slots below (five, then
     * four in the gaps), like the Bedrock tree.
     */
    public static edu.chemistry.client.legacy.StationLayout layout(boolean sd) {
        var l = new edu.chemistry.client.legacy.StationLayout(sd);
        int g = l.slot;
        int inX = (l.width - g) / 2;
        int inY = l.s(24);
        l.put(0, inX, inY, g);
        int step = g + l.s(9);
        int start = (l.width - (4 * step + g)) / 2;
        int row1 = inY + g + l.s(15);
        int row2 = row1 + g + l.s(7);
        for (int i = 0; i < 9; i++) {
            int x = i < 5 ? start + i * step : start + step / 2 + (i - 5) * step;
            l.put(1 + i, x, i < 5 ? row1 : row2, g);
        }
        l.part("bus", start + g / 2, inY + g + (row1 - inY - g) / 2, 4 * step, 0);
        int labelY = row2 + g + l.s(10);
        if (sd) {
            // no room beside the input on small screens: the warning gets its own line under the elements
            l.part("unknown", -1, row2 + g + 2, 0, 0);
            labelY += 10;
        } else {
            l.part("unknown", inX + g + l.s(6), inY + (g - 8) / 2, 0, 0);
        }
        l.inventory(10, l.titleX, labelY);
        return l;
    }

    @Override
    protected edu.chemistry.client.legacy.StationLayout legacyLayout(boolean sd) {
        return layout(sd);
    }

    @Override
    protected java.util.function.BooleanSupplier slotWarning(int index) {
        return index == 0 ? () -> this.menu.isLocked() || this.menu.isUnknown() : null;
    }

    /** the lines from the input down to each element slot */
    private void drawTree(GuiGraphicsExtractor graphics) {
        var l = this.legacy;
        int t = l.sd ? 1 : 2;
        int color = 0xFF8B8B8B;
        int[] input = l.slots.get(0);
        int[] bus = l.part("bus");
        int x0 = this.leftPos;
        int y0 = this.topPos;
        int cx = input[0] + input[2] / 2;
        graphics.fill(x0 + cx - t / 2, y0 + input[1] + input[2], x0 + cx - t / 2 + t, y0 + bus[1] + t, color);
        graphics.fill(x0 + bus[0] - t / 2, y0 + bus[1], x0 + bus[0] + bus[2] + t - t / 2, y0 + bus[1] + t, color);
        for (int i = 1; i <= 9; i++) {
            int[] out = l.slots.get(i);
            int ox = out[0] + out[2] / 2;
            graphics.fill(x0 + ox - t / 2, y0 + bus[1], x0 + ox - t / 2 + t, y0 + out[1], color);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        if (this.legacy != null) {
            // the red frame on the input comes from Legacy4J (slotWarning)
            this.drawTree(graphics);
        } else if (this.menu.isLocked() || this.menu.isUnknown()) {
            int x = this.leftPos + MaterialReducerMenu.INPUT_X - 1;
            int y = this.topPos + MaterialReducerMenu.INPUT_Y - 1;
            graphics.fill(x, y, x + 18, y + 18, 0x80FF2020);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        this.labels(() -> {
            this.drawTitle(graphics);
            graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, this.labelColor(), false);
            if (this.menu.isUnknown()) {
                Component text = Component.translatable("gui.edu.cannot_reduce");
                if (this.legacy != null) {
                    int[] p = this.legacy.part("unknown");
                    int x = p[0] < 0 ? (this.imageWidth - this.font.width(text)) / 2 : Math.min(p[0], this.imageWidth - 6 - this.font.width(text));
                    graphics.text(this.font, text, x, p[1], 0xFFA02020, false);
                } else {
                    graphics.text(this.font, text, 120, 15, 0xFFA02020, false);
                }
            }
        });
    }
}
