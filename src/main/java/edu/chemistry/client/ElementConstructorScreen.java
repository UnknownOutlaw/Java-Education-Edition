package edu.chemistry.client;

import edu.chemistry.EduMod;
import edu.chemistry.menu.ElementConstructorMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;

/**
 * Bedrock layout: three columns (p, e, n) each with a number box, +, a vertical slider and -, then a Bohr model of the
 * atom, the microscope and the output slot.
 */
public class ElementConstructorScreen extends EduContainerScreen<ElementConstructorMenu> {
    /** column order on screen: protons, electrons, neutrons (like Bedrock) */
    private static final int[] ORDER = {ElementConstructorMenu.PROTONS, ElementConstructorMenu.ELECTRONS, ElementConstructorMenu.NEUTRONS};
    private static final String[] LABELS = {"p", "e", "n"};
    /** Bedrock's $proton_color, $electron_color, $neutron_color */
    private static final int[] COLORS = {0xFF527CE2, 0xFFE26652, 0xFFE4B257};
    private static final int[] ICON_U = {0, 10, 20};
    private static final int COL_X = 4;
    private static final int COL_W = 26;
    private static final int SLIDER_Y = 70;
    private static final int SLIDER_H = 128;

    private final VerticalSlider[] sliders = new VerticalSlider[3];
    private final EditBox[] boxes = new EditBox[3];
    private boolean syncing;

    public ElementConstructorScreen(ElementConstructorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, EduMod.id("textures/gui/container/element_constructor.png"), 250, 220);
    }

    @Override
    protected void init() {
        super.init();
        for (int col = 0; col < 3; col++) {
            final int which = ORDER[col];
            final int c = col;
            int x = this.leftPos + COL_X + col * COL_W;
            EditBox box = new EditBox(this.font, x + 1, this.topPos + 37, 22, 9, Component.translatable("gui.edu.particle." + LABELS[col]));
            box.setMaxLength(3);
            box.setCentered(true);
            // no built-in border: it left too little room, so three-digit numbers didn't fit
            box.setBordered(false);
            box.setValue(String.valueOf(this.menu.get(which)));
            box.setResponder(text -> this.typed(c, which, text));
            this.boxes[col] = this.addRenderableWidget(box);
            this.addRenderableWidget(Button.builder(Component.literal("+"), b -> this.change(which, this.menu.get(which) + 1))
                .bounds(x + 6, this.topPos + 53, 12, 12).build());
            this.sliders[col] = this.addRenderableWidget(new VerticalSlider(which, COLORS[col], x + 6, this.topPos + SLIDER_Y, 12, SLIDER_H));
            this.addRenderableWidget(Button.builder(Component.literal("-"), b -> this.change(which, this.menu.get(which) - 1))
                .bounds(x + 6, this.topPos + 203, 12, 12).build());
        }
    }

    private void typed(int col, int which, String text) {
        if (this.syncing) return;
        String digits = text.replaceAll("[^0-9]", "");
        if (!digits.equals(text)) {
            this.boxes[col].setValue(digits);
            return;
        }
        if (digits.isEmpty()) return;
        this.change(which, Integer.parseInt(digits));
    }

    private void change(int which, int value) {
        value = Mth.clamp(value, 0, ElementConstructorMenu.max(which));
        if (value == this.menu.get(which)) return;
        int id = ElementConstructorMenu.buttonId(which, value);
        this.menu.clickMenuButton(this.minecraft.player, id);
        this.sendButton(id);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.syncing = true;
        for (int col = 0; col < 3; col++) {
            int v = this.menu.get(ORDER[col]);
            if (this.boxes[col] != null && !this.boxes[col].isFocused() && !this.boxes[col].getValue().equals(String.valueOf(v))) {
                this.boxes[col].setValue(String.valueOf(v));
            }
        }
        this.syncing = false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        if (!this.menu.getSlot(0).hasItem()) {
            this.blit(graphics, this.leftPos + ElementConstructorMenu.OUTPUT_X, this.topPos + ElementConstructorMenu.OUTPUT_Y, 62, 224, 16, 16);
        }
        for (int col = 0; col < 3; col++) {
            int x = this.leftPos + COL_X + col * COL_W;
            int y = this.topPos + 31;
            boolean focused = this.boxes[col] != null && this.boxes[col].isFocused();
            graphics.fill(x, y, x + 24, y + 20, focused ? 0xFFFFFFFF : 0xFFA0A0A0);
            graphics.fill(x + 1, y + 1, x + 23, y + 19, 0xFF000000);
        }
        this.drawBohrModel(graphics, a);
    }

    /** Nucleus of protons and neutrons, electrons on shells around it (2, 8, 18, 32, 32, 18, 8). */
    private void drawBohrModel(GuiGraphicsExtractor graphics, float partial) {
        int cx = this.leftPos + 138;
        int cy = this.topPos + 73;
        int p = this.menu.get(ElementConstructorMenu.PROTONS);
        int n = this.menu.get(ElementConstructorMenu.NEUTRONS);
        int e = this.menu.get(ElementConstructorMenu.ELECTRONS);
        int[] capacity = {2, 8, 18, 32, 32, 18, 8};
        int shells = 0;
        for (int left = e, i = 0; left > 0 && i < capacity.length; i++) {
            shells++;
            left -= capacity[i];
        }
        float time = (this.minecraft.player.tickCount + partial) * 0.02F;
        // shell rings
        for (int s = 0; s < shells; s++) {
            int r = 20 + s * 5;
            for (int k = 0; k < 64; k++) {
                double ang = k * Math.PI * 2 / 64;
                int x = cx + (int) Math.round(Math.cos(ang) * r);
                int y = cy + (int) Math.round(Math.sin(ang) * r);
                graphics.fill(x, y, x + 1, y + 1, 0x80303030);
            }
        }
        // nucleus: protons and neutrons packed in a little spiral, capped so it stays inside the first shell
        int shown = Math.min(p + n, 24);
        int shownP = p + n == 0 ? 0 : Math.round(shown * (p / (float) (p + n)));
        for (int i = shown - 1; i >= 0; i--) {
            double r = Math.sqrt(i) * 3.0;
            double ang = i * 2.39996;
            int x = cx + (int) Math.round(Math.cos(ang) * r) - 4;
            int y = cy + (int) Math.round(Math.sin(ang) * r) - 4;
            // spread the protons evenly through the nucleus
            boolean proton = (i + 1) * shownP / shown > i * shownP / shown;
            this.blit(graphics, x, y, proton ? ICON_U[0] : ICON_U[2], 224, 9, 9);
        }
        // electrons
        int left = e;
        for (int s = 0; s < shells; s++) {
            int count = Math.min(left, capacity[s]);
            left -= count;
            int r = 20 + s * 5;
            double spin = time * (s % 2 == 0 ? 1 : -1) / (1 + s * 0.3);
            for (int k = 0; k < count; k++) {
                double ang = spin + k * Math.PI * 2 / count;
                int x = cx + (int) Math.round(Math.cos(ang) * r) - 2;
                int y = cy + (int) Math.round(Math.sin(ang) * r) - 2;
                graphics.fill(x, y, x + 4, y + 4, COLORS[1]);
                graphics.fill(x + 1, y, x + 3, y + 1, 0xFFF29A8C);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        this.drawTitle(graphics);
        for (int col = 0; col < 3; col++) {
            int x = COL_X + col * COL_W;
            graphics.text(this.font, LABELS[col], x + 6, 19, TITLE_COLOR, false);
            this.blit(graphics, x + 13, 18, ICON_U[col], 224, 9, 9);
        }
    }

    /** Bedrock-style vertical slider: bottom = 0, coloured fill, drag or click to set. */
    private class VerticalSlider extends AbstractWidget {
        private final int which;
        private final int color;

        VerticalSlider(int which, int color, int x, int y, int w, int h) {
            super(x, y, w, h, Component.empty());
            this.which = which;
            this.color = color;
        }

        private double fraction() {
            return ElementConstructorScreen.this.menu.get(this.which) / (double) ElementConstructorMenu.max(this.which);
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            int x = this.getX();
            int y = this.getY();
            int w = this.getWidth();
            int h = this.getHeight();
            boolean hover = this.isHovered();
            graphics.fill(x, y, x + w, y + h, 0xFF1E1E1E);
            graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, hover ? 0xFF5A5A5A : 0xFF4A4A4A);
            int fillH = (int) Math.round((h - 2) * this.fraction());
            graphics.fill(x + 1, y + h - 1 - fillH, x + w - 1, y + h - 1, this.color);
            // handle 16x10
            int hy = y + h - 1 - fillH - 5;
            hy = Mth.clamp(hy, y - 4, y + h - 6);
            graphics.fill(x - 2, hy, x + w + 2, hy + 10, 0xFF1E1E1E);
            graphics.fill(x - 1, hy + 1, x + w + 1, hy + 9, hover ? 0xFFFFFFFF : 0xFFD0D0D0);
            graphics.fill(x - 1, hy + 8, x + w + 1, hy + 9, 0xFF8B8B8B);
        }

        private void setFromMouse(double mouseY) {
            double f = 1.0 - (mouseY - (this.getY() + 1)) / (double) (this.getHeight() - 2);
            int v = (int) Math.round(Mth.clamp(f, 0.0, 1.0) * ElementConstructorMenu.max(this.which));
            ElementConstructorScreen.this.change(this.which, v);
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            this.setFromMouse(event.y());
        }

        @Override
        protected void onDrag(MouseButtonEvent event, double dx, double dy) {
            this.setFromMouse(event.y());
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, Component.literal(LABELS[this.which == ElementConstructorMenu.PROTONS ? 0
                : this.which == ElementConstructorMenu.ELECTRONS ? 1 : 2] + " " + ElementConstructorScreen.this.menu.get(this.which)));
        }
    }
}
