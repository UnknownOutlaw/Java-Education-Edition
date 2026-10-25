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

    /**
     * Console layout: the three particle columns (number, +, slider, -) down the left, the atom in a sunken window above
     * the inventory, and an arrow to the element in a big result frame like the furnace's.
     */
    public static edu.chemistry.client.legacy.StationLayout layout(boolean sd) {
        var l = new edu.chemistry.client.legacy.StationLayout(sd);
        int colW = l.s(28);
        int boxW = l.s(24);
        int boxH = l.s(20);
        int btn = sd ? 9 : 12;
        int labelY = l.s(24);
        int boxY = Math.max(l.s(35), labelY + 10);
        int plusY = boxY + boxH + l.s(4);
        int areaX = l.titleX + 3 * colW + l.s(8);
        int[] d = l.part("display", areaX, l.s(24), l.s(109), l.s(104));
        l.inventory(1, areaX, d[1] + d[3] + l.s(6));
        l.width = areaX + 9 * l.slot + (sd ? 6 : 11);
        int minusY = l.height - l.s(14) - btn;
        int sliderY = plusY + btn + l.s(4);
        int sliderW = l.s(12);
        for (int c = 0; c < 3; c++) {
            int x = l.titleX + c * colW;
            // the letter and the particle icon over each column; small screens only have room for the icon
            if (sd) l.part("label" + c, -1, labelY, x + (boxW - 9) / 2, labelY - 1);
            else l.part("label" + c, x + 6, labelY, x + 13, labelY - 1);
            l.part("box" + c, x, boxY, boxW, boxH);
            l.part("plus" + c, x + (boxW - btn) / 2, plusY, btn, btn);
            l.part("slider" + c, x + (boxW - sliderW) / 2, sliderY, sliderW, minusY - l.s(4) - sliderY);
            l.part("minus" + c, x + (boxW - btn) / 2, minusY, btn, btn);
        }
        int[] arrow = l.part("arrow", d[0] + d[2] + l.s(4), d[1] + (d[3] - l.arrowHeight()) / 2, l.arrowWidth(), l.arrowHeight());
        int res = l.resultSize();
        l.put(0, arrow[0] + arrow[2] + l.s(4), d[1] + (d[3] - res) / 2, res);
        return l;
    }

    @Override
    protected edu.chemistry.client.legacy.StationLayout legacyLayout(boolean sd) {
        return layout(sd);
    }

    /** where part of particle column col goes, relative to the panel: the console layout's, or the normal one */
    private int[] col(String part, int col) {
        if (this.legacy != null) return this.legacy.part(part + col);
        int x = COL_X + col * COL_W;
        return switch (part) {
            case "label" -> new int[] {x + 6, 19, x + 13, 18};
            case "box" -> new int[] {x, 31, 24, 20};
            case "plus" -> new int[] {x + 6, 53, 12, 12};
            case "slider" -> new int[] {x + 6, SLIDER_Y, 12, SLIDER_H};
            default -> new int[] {x + 6, 203, 12, 12};
        };
    }

    @Override
    protected void init() {
        super.init();
        for (int col = 0; col < 3; col++) {
            final int which = ORDER[col];
            final int c = col;
            int[] bx = this.col("box", col);
            int[] plus = this.col("plus", col);
            int[] slider = this.col("slider", col);
            int[] minus = this.col("minus", col);
            EditBox box = new EditBox(this.font, this.leftPos + bx[0] + 1, this.topPos + bx[1] + (bx[3] - 8) / 2, bx[2] - 2, 9,
                Component.translatable("gui.edu.particle." + LABELS[col]));
            box.setMaxLength(3);
            box.setCentered(true);
            // no built-in border: it left too little room, so three-digit numbers didn't fit
            box.setBordered(false);
            box.setValue(String.valueOf(this.menu.get(which)));
            box.setResponder(text -> this.typed(c, which, text));
            this.boxes[col] = this.addRenderableWidget(box);
            this.addRenderableWidget(Button.builder(Component.literal("+"), b -> this.change(which, this.menu.get(which) + 1))
                .bounds(this.leftPos + plus[0], this.topPos + plus[1], plus[2], plus[3]).build());
            this.sliders[col] = this.addRenderableWidget(new VerticalSlider(which, COLORS[col], this.leftPos + slider[0], this.topPos + slider[1],
                slider[2], slider[3]));
            this.addRenderableWidget(Button.builder(Component.literal("-"), b -> this.change(which, this.menu.get(which) - 1))
                .bounds(this.leftPos + minus[0], this.topPos + minus[1], minus[2], minus[3]).build());
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
        if (this.legacy == null && !this.menu.getSlot(0).hasItem()) {
            this.blit(graphics, this.leftPos + ElementConstructorMenu.OUTPUT_X, this.topPos + ElementConstructorMenu.OUTPUT_Y, 62, 224, 16, 16);
        }
        for (int col = 0; col < 3; col++) {
            int[] b = this.col("box", col);
            int x = this.leftPos + b[0];
            int y = this.topPos + b[1];
            boolean focused = this.boxes[col] != null && this.boxes[col].isFocused();
            graphics.fill(x, y, x + b[2], y + b[3], focused ? 0xFFFFFFFF : 0xFFA0A0A0);
            graphics.fill(x + 1, y + 1, x + b[2] - 1, y + b[3] - 1, 0xFF000000);
        }
        if (this.legacy != null) {
            int[] d = this.legacy.part("display");
            edu.chemistry.client.legacy.LegacyUi.recess(graphics, this.leftPos + d[0], this.topPos + d[1], d[2], d[3]);
            int[] p = this.legacy.part("arrow");
            edu.chemistry.client.legacy.LegacyUi.arrow(graphics, this.leftPos + p[0], this.topPos + p[1], this.legacy.sd);
            graphics.pose().pushMatrix();
            graphics.pose().translate(this.leftPos + d[0] + d[2] / 2.0F, this.topPos + d[1] + d[3] / 2.0F);
            if (this.legacy.sd) graphics.pose().scale(13 / 21.0F, 13 / 21.0F);
            this.drawBohrModel(graphics, a, 0, 0);
            graphics.pose().popMatrix();
        } else {
            this.drawBohrModel(graphics, a, this.leftPos + 138, this.topPos + 73);
        }
    }

    /** in the console layout the empty element frame still shows the element icon, on top of Legacy4J's slot frame */
    @Override
    protected void extractSlots(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractSlots(graphics, mouseX, mouseY);
        net.minecraft.world.inventory.Slot out = this.menu.getSlot(0);
        if (this.legacy != null && !out.hasItem()) {
            int size = this.legacy.slots.get(0)[2];
            this.blit(graphics, out.x + (size - 16) / 2, out.y + (size - 16) / 2, 62, 224, 16, 16);
        }
    }

    /** Nucleus of protons and neutrons, electrons on shells around it (2, 8, 18, 32, 32, 18, 8). */
    private void drawBohrModel(GuiGraphicsExtractor graphics, float partial, int cx, int cy) {
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
        this.labels(() -> {
            this.drawTitle(graphics);
            if (this.legacy != null) {
                graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, this.labelColor(), false);
            }
            for (int col = 0; col < 3; col++) {
                int[] p = this.col("label", col);
                if (p[0] >= 0) graphics.text(this.font, LABELS[col], p[0], p[1], this.labelColor(), false);
                this.blit(graphics, p[2], p[3], ICON_U[col], 224, 9, 9);
            }
        });
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
