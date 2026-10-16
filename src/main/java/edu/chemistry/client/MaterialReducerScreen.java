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

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        if (this.menu.isLocked() || this.menu.isUnknown()) {
            int x = this.leftPos + MaterialReducerMenu.INPUT_X - 1;
            int y = this.topPos + MaterialReducerMenu.INPUT_Y - 1;
            graphics.fill(x, y, x + 18, y + 18, 0x80FF2020);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        this.drawTitle(graphics);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, TITLE_COLOR, false);
        if (this.menu.isUnknown()) {
            graphics.text(this.font, Component.translatable("gui.edu.cannot_reduce"), 120, 15, 0xFFA02020, false);
        }
    }
}
