package edu.chemistry.client.photo;

import edu.chemistry.EduMod;
import edu.chemistry.photo.PhotoNetwork;
import edu.chemistry.photo.Photos;
import edu.chemistry.registry.EduItems;
import java.io.IOException;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

/**
 * Education's portfolio (portfolio_screen): an open book showing two pictures at a time, each with its page number, a
 * caption you can edit, a trash button and a "save as photo" button; page arrows in the bottom corners; "Add Photo"
 * and "Export Portfolio" underneath. Add Photo shows the photo items in your inventory to pick from.
 */
public class PortfolioScreen extends Screen {
    private static final int BOOK_W = 320;
    private static final int BOOK_H = 207;
    private static final int PICK_W = 225;
    private static final int PICK_H = 231;
    private static final int TEXT = 0xFFBBA286;

    static Identifier ui(String name) {
        return EduMod.id("textures/gui/portfolio/" + name + ".png");
    }

    private final PortfolioStore store = PortfolioStore.current();
    private int spread;
    private boolean picking;
    private int bookX;
    private int bookY;

    public PortfolioScreen() {
        super(Component.translatable("item.edu.portfolio"));
    }

    private List<PortfolioStore.Photo> photos() {
        return this.store.photos();
    }

    @Override
    protected void init() {
        this.bookX = (this.width - BOOK_W) / 2;
        this.bookY = Math.max(4, (this.height - BOOK_H - 26) / 2);
        if (this.picking) {
            this.initPick();
            return;
        }
        int spreads = Math.max(1, (this.photos().size() + 1) / 2);
        this.spread = Math.min(this.spread, spreads - 1);
        for (int side = 0; side < 2; side++) {
            int index = this.spread * 2 + side;
            if (index >= this.photos().size()) continue;
            PortfolioStore.Photo p = this.photos().get(index);
            int px = this.bookX + side * (BOOK_W / 2);
            int squareX = px + 20;
            int squareY = this.bookY + 41;
            MultiLineEditBox caption = MultiLineEditBox.builder()
                .setX(squareX + 9).setY(squareY + 120 - 24)
                .setPlaceholder(Component.translatable("portfolioScreen.caption"))
                .setTextColor(TEXT).setCursorColor(TEXT).setTextShadow(false).setShowBackground(false)
                .build(this.font, 102, 22, Component.translatable("portfolioScreen.caption"));
            caption.setCharacterLimit(1000);
            caption.setValue(p.caption);
            caption.setValueListener(s -> p.caption = s);
            this.addRenderableWidget(caption);
            // trash and "save as photo" sit next to each other near the bottom of the page
            int trashX = side == 0 ? this.bookX + Math.round(BOOK_W * 0.22F) - 9 : this.bookX + BOOK_W - Math.round(BOOK_W * 0.28F) + 9 - 18;
            int convertX = side == 0 ? this.bookX + Math.round(BOOK_W * 0.28F) - 9 : this.bookX + BOOK_W - Math.round(BOOK_W * 0.22F) + 9 - 18;
            int by = this.bookY + BOOK_H - 11 - 18;
            this.addRenderableWidget(new IconButton(trashX, by, 18, 18, "book_trash_default", "trash_hover", "trash_pressed", 16, 16,
                Component.translatable("gui.edu.portfolio.delete"), () -> {
                    this.store.remove(p);
                    this.rebuildWidgets();
                }));
            this.addRenderableWidget(new IconButton(convertX, by + 2, 18, 17, "portfolio_convertpicture_default", "portfolio_convertpicture_hover",
                "portfolio_convertpicture_pressed", 18, 17, Component.translatable("gui.edu.portfolio.to_photo"), () -> this.toPhotoItem(p)));
        }
        if (this.spread > 0) {
            this.addRenderableWidget(new IconButton(this.bookX + 6, this.bookY + BOOK_H - 8 - 24, 24, 24, "portfolio_pageleft_default",
                "portfolio_pageleft_hover", "portfolio_pageleft_pressed", 24, 24, Component.translatable("spectatorMenu.previous_page"), () -> {
                    this.spread--;
                    this.rebuildWidgets();
                }));
        }
        if (this.spread < spreads - 1) {
            this.addRenderableWidget(new IconButton(this.bookX + BOOK_W - 6 - 24, this.bookY + BOOK_H - 8 - 24, 24, 24, "portfolio_pageright_default",
                "portfolio_pageright_hover", "portfolio_pageright_pressed", 24, 24, Component.translatable("spectatorMenu.next_page"), () -> {
                    this.spread++;
                    this.rebuildWidgets();
                }));
        }
        int c = this.width / 2;
        int by = this.bookY + BOOK_H + 4;
        this.addRenderableWidget(Button.builder(Component.translatable("portfolioScreen.addPhoto"), b -> {
            this.store.save();
            this.picking = true;
            this.rebuildWidgets();
        }).bounds(c - 72 - 55, by, 110, 20).build());
        Button export = this.addRenderableWidget(Button.builder(Component.translatable("portfolioScreen.export"), b -> this.export())
            .bounds(c + 72 - 55, by, 110, 20).build());
        export.visible = !this.photos().isEmpty();
        this.addRenderableWidget(new IconButton(this.bookX + BOOK_W - 16, this.bookY + 2, 14, 14, "close_button_default", "close_button_hover",
            "close_button_pressed", 15, 15, Component.translatable("gui.done"), this::onClose));
    }

    /** the inventory's photo items, with their slots */
    private List<int[]> inventoryPhotos() {
        List<int[]> out = new ArrayList<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return out;
        var inv = mc.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(EduItems.PHOTO) && Photos.image(s).isPresent()) out.add(new int[] {i});
        }
        return out;
    }

    private void initPick() {
        int px = (this.width - PICK_W) / 2;
        int py = Math.max(4, (this.height - PICK_H) / 2);
        Minecraft mc = Minecraft.getInstance();
        List<int[]> photos = this.inventoryPhotos();
        for (int k = 0; k < photos.size() && k < 4; k++) {
            int slot = photos.get(k)[0];
            ItemStack stack = mc.player.getInventory().getItem(slot);
            int gx = px + 8 + (k % 2) * 104 + 2;
            int gy = py + 30 + (k / 2) * 100;
            this.addRenderableWidget(new PhotoPickButton(gx, gy, stack, () -> {
                byte[] png = Photos.image(stack).orElse(null);
                if (png == null) return;
                this.store.add(png, Photos.caption(stack));
                send(PhotoNetwork.TAKE + slot + ":" + Photos.id(stack));
                this.picking = false;
                this.spread = Math.max(0, (this.photos().size() - 1) / 2);
                this.rebuildWidgets();
            }));
        }
        this.addRenderableWidget(new IconButton(px + PICK_W - 16, py + 2, 14, 14, "close_button_default", "close_button_hover",
            "close_button_pressed", 15, 15, Component.translatable("gui.back"), () -> {
                this.picking = false;
                this.rebuildWidgets();
            }));
    }

    private static void send(String... lines) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;
        String[] l = {lines[0], lines.length > 1 ? lines[1] : "", lines.length > 2 ? lines[2] : "", lines.length > 3 ? lines[3] : ""};
        mc.getConnection().send(new ServerboundSignUpdatePacket(BlockPos.ZERO, true, l[0], l[1], l[2], l[3]));
    }

    /**
     * The clearest copies a photo item can carry, tried in turn until one fits in {@link Photos#MAX_BYTES}: JPEGs as
     * big as the picture itself (up to 1024 pixels), so the photo looks the same as the picture in the portfolio.
     */
    private static final int[][] PHOTO_ITEM_COPIES = {{1024, 92}, {768, 92}, {640, 90}, {512, 88}, {512, 80}, {384, 80}};

    /** turns a portfolio picture into a photo item in your inventory (and takes it out of the portfolio) */
    private void toPhotoItem(PortfolioStore.Photo p) {
        try (var img = PhotoCodec.decode(this.store.bytes(p))) {
            byte[] data = photoItemCopy(img);
            if (data == null) {
                EduMod.LOG.warn("Couldn't make a photo item: the picture is too big to carry");
                return;
            }
            for (String[] lines : PhotoNetwork.encodeUpload(p.id, data, p.caption)) send(lines);
            this.store.remove(p);
            this.rebuildWidgets();
        } catch (IOException e) {
            EduMod.LOG.warn("Couldn't make a photo item", e);
        }
    }

    /** the picture a photo item carries: the clearest JPEG that fits (or a small PNG if this Java can't write JPEGs) */
    private static byte @org.jspecify.annotations.Nullable [] photoItemCopy(com.mojang.blaze3d.platform.NativeImage img) throws IOException {
        try {
            for (int[] copy : PHOTO_ITEM_COPIES) {
                byte[] jpeg = PhotoCodec.jpeg(img, copy[0], copy[1] / 100.0F);
                if (jpeg.length <= Photos.MAX_BYTES) return jpeg;
            }
        } catch (IOException e) {
            EduMod.LOG.warn("Couldn't write a JPEG photo; saving a smaller PNG instead", e);
        }
        for (int size : new int[] {256, 128}) {
            byte[] png = PhotoCodec.png(img, size);
            if (png.length <= Photos.MAX_BYTES) return png;
        }
        return null;
    }

    private void export() {
        this.store.save();
        Minecraft mc = Minecraft.getInstance();
        Path out = mc.gameDirectory.toPath().resolve("screenshots")
            .resolve("portfolio-" + new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(new Date()) + ".pdf");
        try {
            PdfExport.export(this.store, out);
            if (mc.player != null) {
                Component file = Component.literal(out.getFileName().toString()).withStyle(ChatFormatting.UNDERLINE)
                    .withStyle(s -> s.withClickEvent(new ClickEvent.OpenFile(out.toAbsolutePath())));
                mc.player.sendSystemMessage(Component.translatable("message.edu.portfolio_exported", file));
            }
        } catch (IOException e) {
            EduMod.LOG.warn("Couldn't export the portfolio", e);
            if (mc.player != null) mc.player.sendSystemMessage(Component.translatable("message.edu.portfolio_export_failed").withStyle(ChatFormatting.RED));
        }
    }

    // ------------------------------------------------------------------ drawing

    static void blit(GuiGraphicsExtractor g, String tex, int x, int y, int w, int h, int texW, int texH) {
        g.blit(RenderPipelines.GUI_TEXTURED, ui(tex), x, y, 0.0F, 0.0F, w, h, texW, texH, texW, texH);
    }

    /** Bedrock's book_frame nine-slice (42 x 42, 21 px corners) */
    static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        Identifier t = ui("book_frame");
        int c = 21;
        g.blit(RenderPipelines.GUI_TEXTURED, t, x, y, 0, 0, c, c, c, c, 42, 42);
        g.blit(RenderPipelines.GUI_TEXTURED, t, x + w - c, y, 21, 0, c, c, c, c, 42, 42);
        g.blit(RenderPipelines.GUI_TEXTURED, t, x, y + h - c, 0, 21, c, c, c, c, 42, 42);
        g.blit(RenderPipelines.GUI_TEXTURED, t, x + w - c, y + h - c, 21, 21, c, c, c, c, 42, 42);
        g.blit(RenderPipelines.GUI_TEXTURED, t, x + c, y, 20, 0, w - 2 * c, c, 2, c, 42, 42);
        g.blit(RenderPipelines.GUI_TEXTURED, t, x + c, y + h - c, 20, 21, w - 2 * c, c, 2, c, 42, 42);
        g.blit(RenderPipelines.GUI_TEXTURED, t, x, y + c, 0, 20, c, h - 2 * c, c, 2, 42, 42);
        g.blit(RenderPipelines.GUI_TEXTURED, t, x + w - c, y + c, 21, 20, c, h - 2 * c, c, 2, 42, 42);
        g.blit(RenderPipelines.GUI_TEXTURED, t, x + c, y + c, 20, 20, w - 2 * c, h - 2 * c, 2, 2, 42, 42);
    }

    /** a picture fitted (keeping its shape) into the box, with Bedrock's photo corners */
    static void picture(GuiGraphicsExtractor g, Identifier tex, int x, int y, int w, int h, boolean corners) {
        int[] size = PortfolioStore.size(tex);
        float scale = Math.min(w / (float) size[0], h / (float) size[1]);
        int dw = Math.round(size[0] * scale);
        int dh = Math.round(size[1] * scale);
        int dx = x + (w - dw) / 2;
        int dy = y;
        g.blit(RenderPipelines.GUI_TEXTURED, tex, dx, dy, 0.0F, 0.0F, dw, dh, size[0], size[1], size[0], size[1]);
        if (corners) {
            blit(g, "photo_corner_tl", dx - 1, dy - 1, 10, 10, 19, 19);
            blit(g, "photo_corner_tr", dx + dw - 9, dy - 1, 10, 10, 19, 19);
            blit(g, "photo_corner_bl", dx - 1, dy + dh - 9, 10, 10, 19, 19);
            blit(g, "photo_corner_br", dx + dw - 9, dy + dh - 9, 10, 10, 19, 19);
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        if (this.picking) {
            int px = (this.width - PICK_W) / 2;
            int py = Math.max(4, (this.height - PICK_H) / 2);
            frame(g, px, py, PICK_W, PICK_H);
            return;
        }
        frame(g, this.bookX, this.bookY, BOOK_W, BOOK_H);
        // binding down the middle, spine caps top and bottom
        int mid = this.bookX + BOOK_W / 2;
        g.blit(RenderPipelines.GUI_TEXTURED, ui("book_binding"), mid - 2, this.bookY + 7, 0, 0, 5, BOOK_H - 7, 5, 2, 5, 2);
        g.blit(RenderPipelines.GUI_TEXTURED, ui("book_spine"), mid - 3, this.bookY + 1, 0, 0, 7, 6, 8, 1, 8, 1);
        g.blit(RenderPipelines.GUI_TEXTURED, ui("book_spine"), mid - 3, this.bookY + BOOK_H - 7, 0, 0, 7, 6, 8, 1, 8, 1);
        for (int side = 0; side < 2; side++) {
            int index = this.spread * 2 + side;
            if (index >= this.photos().size()) continue;
            Identifier tex = this.store.texture(this.photos().get(index));
            if (tex != null) picture(g, tex, this.bookX + side * (BOOK_W / 2) + 20, this.bookY + 41, 120, 120 - 28, true);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (this.picking) {
            int px = (this.width - PICK_W) / 2;
            int py = Math.max(4, (this.height - PICK_H) / 2);
            g.centeredText(this.font, Component.translatable("book.headerInventory"), px + PICK_W / 2, py + 12, TEXT);
            if (this.inventoryPhotos().isEmpty()) {
                List<FormattedCharSequence> lines = this.font.split(Component.translatable("portfolioScreen.noInventory"), PICK_W / 2);
                for (int i = 0; i < lines.size(); i++) {
                    g.centeredText(this.font, lines.get(i), px + PICK_W / 2, py + PICK_H / 2 - lines.size() * 5 + i * 10, TEXT);
                }
            }
            return;
        }
        if (this.photos().isEmpty()) {
            List<FormattedCharSequence> lines = this.font.split(FormattedText.of(Component.translatable("portfolioScreen.nopics0").getString()), BOOK_W / 4);
            for (int i = 0; i < lines.size(); i++) {
                g.centeredText(this.font, lines.get(i), this.bookX + BOOK_W / 4, this.bookY + BOOK_H / 2 - lines.size() * 5 + i * 10, TEXT);
            }
        }
        for (int side = 0; side < 2; side++) {
            int index = this.spread * 2 + side;
            if (index >= this.photos().size()) continue;
            int cx = this.bookX + side * (BOOK_W / 2) + BOOK_W / 4;
            g.centeredText(this.font, Component.translatable("portfolioScreen.page", index + 1), cx, this.bookY + BOOK_H / 2 - 88, TEXT);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        this.store.save();
        super.removed();
    }

    /** a button drawn with Bedrock's default / hover / pressed images */
    static final class IconButton extends AbstractButton {
        private final String normal;
        private final String hover;
        private final String pressed;
        private final int texW;
        private final int texH;
        private final Runnable action;

        IconButton(int x, int y, int w, int h, String normal, String hover, String pressed, int texW, int texH, Component name, Runnable action) {
            super(x, y, w, h, name);
            this.normal = normal;
            this.hover = hover;
            this.pressed = pressed;
            this.texW = texW;
            this.texH = texH;
            this.action = action;
            this.setTooltip(net.minecraft.client.gui.components.Tooltip.create(name));
        }

        @Override
        public void onPress(InputWithModifiers input) {
            this.action.run();
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
            String tex = this.isHoveredOrFocused() ? this.hover : this.normal;
            g.blit(RenderPipelines.GUI_TEXTURED, ui(tex), this.getX(), this.getY(), 0.0F, 0.0F, this.width, this.height,
                this.texW, this.texH, this.texW, this.texH);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }

    /** a photo item to add, shown as its picture */
    static final class PhotoPickButton extends AbstractButton {
        private final ItemStack stack;
        private final Runnable action;

        PhotoPickButton(int x, int y, ItemStack stack, Runnable action) {
            super(x, y, 100, 100, Component.translatable("item.edu.photo"));
            this.stack = stack;
            this.action = action;
        }

        @Override
        public void onPress(InputWithModifiers input) {
            this.action.run();
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
            if (this.isHoveredOrFocused()) g.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, 0x40000000);
            byte[] png = Photos.image(this.stack).orElse(null);
            Identifier tex = png == null ? null : PortfolioStore.register("item/" + Photos.id(this.stack), png);
            if (tex != null) picture(g, tex, this.getX() + 4, this.getY() + 4, this.width - 8, this.height - 8, true);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }
    }
}
