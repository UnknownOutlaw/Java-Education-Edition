package edu.chemistry.client;

import edu.chemistry.EduMod;
import edu.chemistry.menu.LabTableMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** Bedrock layout: beaker, flask and jar on top (they animate when you press Combine), nine slots, Combine button. */
public class LabTableScreen extends EduContainerScreen<LabTableMenu> {
    private static final String[] TOOLS = {"beaker", "flask", "jar"};
    private static final int[] FRAMES = {17, 21, 29};
    private static final int TOOLS_X = 13;
    private static final int TOOLS_Y = 14;
    private Button combine;
    /** ticks since Combine was pressed, -1 = idle */
    private int animTicks = -1;

    private final StationRecipeBook book = new StationRecipeBook(edu.chemistry.registry.EduRecipes.LAB);

    @Override
    protected StationRecipeBook recipeBook() {
        return this.book;
    }

    /** left of the Combine button */
    @Override
    protected int[] recipeButtonPos() {
        return new int[] {12, 87};
    }

    @Override
    protected java.util.List<net.minecraft.world.inventory.Slot> gridSlots() {
        return this.menu.slots.subList(0, 9);
    }

    public LabTableScreen(LabTableMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, EduMod.id("textures/gui/container/lab_table.png"), 176, 200);
    }

    /**
     * Console layout: the beaker, flask and jar across the top, the nine ingredient slots in line with the inventory
     * below them, then Combine and the inventory. (The recipe book is hidden with Legacy4J.)
     */
    public static edu.chemistry.client.legacy.StationLayout layout(boolean sd) {
        var l = new edu.chemistry.client.legacy.StationLayout(sd);
        int tool = l.s(45);
        int gap = l.s(24);
        int x0 = (l.width - (3 * tool + 2 * gap)) / 2;
        for (int i = 0; i < 3; i++) l.part("tool" + i, x0 + i * (tool + gap), l.s(26), tool, tool);
        int rowY = l.s(26) + tool + l.s(7);
        for (int i = 0; i < 9; i++) l.put(i, l.titleX + i * l.slot, rowY, l.slot);
        int bw = l.s(100);
        int bh = sd ? 12 : 16;
        int[] combine = l.part("combine", (l.width - bw) / 2, rowY + l.slot + l.s(5), bw, bh);
        l.inventory(9, l.titleX, combine[1] + bh + l.s(6));
        return l;
    }

    @Override
    protected edu.chemistry.client.legacy.StationLayout legacyLayout(boolean sd) {
        return layout(sd);
    }

    @Override
    protected void init() {
        super.init();
        int[] b = this.legacy != null ? this.legacy.part("combine") : new int[] {38, 89, 100, 14};
        this.combine = this.addRenderableWidget(Button.builder(Component.translatable("gui.edu.combine"), btn -> {
            this.sendButton(LabTableMenu.BUTTON_COMBINE);
            this.animTicks = 0;
        }).bounds(this.leftPos + b[0], this.topPos + b[1], b[2], b[3]).build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.combine != null) this.combine.active = this.menu.hasIngredients();
        if (this.animTicks >= 0 && ++this.animTicks > 60) this.animTicks = -1;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        for (int i = 0; i < 3; i++) {
            int x = this.leftPos + TOOLS_X + i * 50 + (i == 2 ? 5 : 0);
            int y = this.topPos + TOOLS_Y;
            float scale = 1.0F;
            if (this.legacy != null) {
                int[] p = this.legacy.part("tool" + i);
                x = this.leftPos + p[0];
                y = this.topPos + p[1];
                scale = p[2] / 45.0F;
            }
            if (this.animTicks >= 0) {
                // 10 fps flip book, like Bedrock
                int frame = Math.min(FRAMES[i] - 1, (int) ((this.animTicks + a) / 2.0F));
                Identifier tex = EduMod.id("textures/gui/lab/" + TOOLS[i] + "_active.png");
                scaledBlit(graphics, tex, x, y, frame * 45.0F, 0.0F, 45, 45, FRAMES[i] * 45, 45, scale);
            } else {
                Identifier tex = EduMod.id("textures/gui/lab/" + TOOLS[i] + "_default.png");
                scaledBlit(graphics, tex, x, y, 0.0F, 0.0F, 45, 45, 45, 45, scale);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        this.labels(() -> {
            this.drawTitle(graphics);
            graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, this.labelColor(), false);
        });
    }
}
