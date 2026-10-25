package edu.chemistry.client.photo;

import edu.chemistry.photo.Photos;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/** Looking at a photo item: the picture on a page with its caption under it. */
public class PhotoViewScreen extends Screen {
    private static final int W = 240;
    private static final int H = 200;
    private final ItemStack stack;

    public PhotoViewScreen(ItemStack stack) {
        super(Component.translatable("item.edu.photo"));
        this.stack = stack.copy();
    }

    @Override
    protected void init() {
        int x = (this.width - W) / 2;
        int y = (this.height - H) / 2;
        this.addRenderableWidget(new PortfolioScreen.IconButton(x + W - 16, y + 2, 14, 14, "close_button_default", "close_button_hover",
            "close_button_pressed", 15, 15, Component.translatable("gui.done"), this::onClose));
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float a) {
        super.renderBackground(g, mouseX, mouseY, a);
        int x = (this.width - W) / 2;
        int y = (this.height - H) / 2;
        PortfolioScreen.frame(g, x, y, W, H);
        byte[] png = Photos.image(this.stack).orElse(null);
        ResourceLocation tex = png == null ? null : PortfolioStore.register("item/" + Photos.id(this.stack), png);
        if (tex != null) PortfolioScreen.picture(g, tex, x + 20, y + 20, W - 40, H - 70, true);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float a) {
        edu.chemistry.client.GuiCompat.blend();
        super.render(g, mouseX, mouseY, a);
        int x = (this.width - W) / 2;
        int y = (this.height - H) / 2;
        List<FormattedCharSequence> lines = this.font.split(FormattedText.of(Photos.caption(this.stack)), W - 40);
        for (int i = 0; i < lines.size() && i < 4; i++) {
            g.drawCenteredString(this.font, lines.get(i), x + W / 2, y + H - 46 + i * 10, 0xFFBBA286);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
