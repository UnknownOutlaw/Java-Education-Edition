package edu.chemistry.client;

import net.minecraft.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;

/**
 * A multi-line text box with its own text colour, optional shadow and optional background. 1.21.1's box always draws
 * light gray shadowed text on a black box; the chalkboard and the portfolio's captions need their own look.
 */
public class StyledTextBox extends MultiLineEditBox {
    private static final int PLACEHOLDER_COLOR = 0xCCE0E0E0;
    private final Font font;
    private final Component placeholder;
    private final int textColor;
    private final boolean shadow;
    private final boolean background;
    private long focusedTime = Util.getMillis();

    public StyledTextBox(Font font, int x, int y, int width, int height, Component placeholder, Component message,
                         int textColor, boolean shadow, boolean background) {
        super(font, x, y, width, height, placeholder, message);
        this.font = font;
        this.placeholder = placeholder;
        this.textColor = textColor;
        this.shadow = shadow;
        this.background = background;
    }

    @Override
    protected void renderBackground(GuiGraphics g) {
        if (this.background) super.renderBackground(g);
    }

    @Override
    protected void renderDecorations(GuiGraphics g) {
        // without a background the box is part of the page: no scroll bar or character count around it
        if (this.background) super.renderDecorations(g);
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (focused) this.focusedTime = Util.getMillis();
    }

    @Override
    protected void renderContents(GuiGraphics g, int mouseX, int mouseY, float a) {
        MultilineTextField field = this.textField;
        String value = field.value();
        int left = this.getX() + this.innerPadding();
        int top = this.getY() + this.innerPadding();
        int lineHeight = this.font.lineHeight;
        if (value.isEmpty() && !this.isFocused()) {
            g.drawWordWrap(this.font, this.placeholder, left, top, this.width - this.totalInnerPadding(), this.background ? PLACEHOLDER_COLOR : fade(this.textColor));
            return;
        }
        int cursor = field.cursor();
        boolean blink = this.isFocused() && (Util.getMillis() - this.focusedTime) / 300L % 2L == 0L;
        boolean inside = cursor < value.length();
        int endX = left;
        int y = top;
        for (MultilineTextField.StringView line : field.iterateLines()) {
            boolean visible = this.withinContentAreaTopBottom(y, y + lineHeight);
            if (visible) {
                String text = value.substring(line.beginIndex(), line.endIndex());
                g.drawString(this.font, text, left, y, this.textColor, this.shadow);
                if (blink && inside && cursor >= line.beginIndex() && cursor <= line.endIndex()) {
                    int cx = left + this.font.width(value.substring(line.beginIndex(), cursor));
                    g.fill(cx, y - 1, cx + 1, y + 1 + lineHeight, this.textColor);
                }
                endX = left + this.font.width(text);
            }
            y += lineHeight;
        }
        if (blink && !inside && this.withinContentAreaTopBottom(y - lineHeight, y)) {
            g.drawString(this.font, "_", endX, y - lineHeight, this.textColor, this.shadow);
        }
        if (field.hasSelection()) {
            MultilineTextField.StringView sel = field.getSelected();
            int ly = top;
            for (MultilineTextField.StringView line : field.iterateLines()) {
                if (sel.beginIndex() <= line.endIndex() && sel.endIndex() >= line.beginIndex() && this.withinContentAreaTopBottom(ly, ly + lineHeight)) {
                    int from = Math.max(sel.beginIndex(), line.beginIndex());
                    int to = Math.min(sel.endIndex(), line.endIndex());
                    int x1 = left + this.font.width(value.substring(line.beginIndex(), from));
                    int x2 = left + this.font.width(value.substring(line.beginIndex(), to));
                    if (x2 > x1) g.fill(RenderType.guiTextHighlight(), x1, ly, x2, ly + lineHeight, 0xFF0000FF);
                }
                ly += lineHeight;
            }
        }
    }

    /** the text colour at about half strength, for the placeholder */
    private static int fade(int argb) {
        return (((argb >>> 24) / 2) << 24) | (argb & 0xFFFFFF);
    }
}
