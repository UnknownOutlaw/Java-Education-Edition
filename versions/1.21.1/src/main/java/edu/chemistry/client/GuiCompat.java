package edu.chemistry.client;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Small helpers for drawing on 1.21.1's GuiGraphics the way the newer builds' code expects. */
public final class GuiCompat {
    private GuiCompat() {
    }

    /** draws with an ARGB tint (the newer builds pass a colour to blit; 1.21.1 sets a shader colour around it) */
    public static void tinted(GuiGraphics g, int argb, Runnable draw) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        g.setColor(((argb >> 16) & 0xFF) / 255.0F, ((argb >> 8) & 0xFF) / 255.0F, (argb & 0xFF) / 255.0F, ((argb >>> 24) & 0xFF) / 255.0F);
        draw.run();
        g.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /*
     * Tooltips: the newer builds queue them to be drawn once the whole screen is done; on 1.21.1 a screen can do the
     * same (so nothing drawn afterwards covers them). Without a screen (the HUD) they're drawn straight away.
     */
    public static void tooltip(GuiGraphics g, Font font, net.minecraft.world.item.ItemStack stack, int x, int y) {
        net.minecraft.client.gui.screens.Screen screen = net.minecraft.client.Minecraft.getInstance().screen;
        if (screen != null) tooltip(g, font, net.minecraft.client.gui.screens.Screen.getTooltipFromItem(net.minecraft.client.Minecraft.getInstance(), stack), x, y);
        else g.renderTooltip(font, stack, x, y);
    }

    public static void tooltip(GuiGraphics g, Font font, Component text, int x, int y) {
        tooltip(g, font, List.of(text), x, y);
    }

    @SuppressWarnings("unchecked")
    public static void tooltip(GuiGraphics g, Font font, List<?> lines, int x, int y) {
        if (lines.isEmpty()) return;
        List<FormattedCharSequence> seq = lines.get(0) instanceof Component
            ? ((List<Component>) lines).stream().map(Component::getVisualOrderText).toList()
            : (List<FormattedCharSequence>) lines;
        net.minecraft.client.gui.screens.Screen screen = net.minecraft.client.Minecraft.getInstance().screen;
        if (screen != null) screen.setTooltipForNextRenderPass(seq);
        else g.renderTooltip(font, seq, x, y);
    }

    /** the newer builds always blend what they draw on screens; 1.21.1 has to be asked to */
    public static void blend() {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
    }
}
