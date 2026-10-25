package edu.chemistry.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import edu.chemistry.client.legacy.LegacyUi;
import edu.chemistry.client.legacy.StationLayout;
import org.jspecify.annotations.Nullable;

/**
 * Container screen with a 256x256 background texture. With Legacy4J installed (ReConsole) it switches to a console
 * layout instead: Legacy4J's panel and slot frames, gray labels, and the station's own {@link #legacyLayout} positions,
 * and the recipe book is hidden.
 */
public abstract class EduContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    /** Java's container title colour (left-aligned at 8,6 like vanilla screens) */
    public static final int TITLE_COLOR = 0xFF404040;
    protected final ResourceLocation texture;
    /** the console layout in use, or null for the normal look */
    protected @Nullable StationLayout legacy;
    private static boolean legacyFailed;

    protected EduContainerScreen(T menu, Inventory inventory, Component title, ResourceLocation texture, int width, int height) {
        super(menu, inventory, title);
        this.imageWidth = width;
        this.imageHeight = height;
        this.texture = texture;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    /** the compound creator and lab table have a recipe book; the other tables return null */
    protected StationRecipeBook recipeBook() {
        return null;
    }

    /** the recipe book in use: none while Legacy4J (ReConsole) is installed */
    private @Nullable StationRecipeBook book() {
        return LegacyUi.installed() ? null : this.recipeBook();
    }

    /** where the recipe book's toggle button sits, relative to the table's top-left corner */
    protected int[] recipeButtonPos() {
        return new int[] {5, 20};
    }

    /** the table's crafting grid slots, in recipe order (used for the book's ghost ingredients) */
    protected java.util.List<net.minecraft.world.inventory.Slot> gridSlots() {
        return java.util.List.of();
    }

    /** the table's output slot, or null */
    protected net.minecraft.world.inventory.@org.jspecify.annotations.Nullable Slot resultSlot() {
        return null;
    }

    /** screen areas outside the table covered by its recipe book, as {x, y, width, height} (JEI keeps clear of them) */
    public java.util.List<int[]> extraAreas() {
        StationRecipeBook book = this.book();
        if (book == null || !book.isOpen()) return java.util.List.of();
        int xo = StationRecipeBook.bookX(this.width);
        int yo = StationRecipeBook.bookY(this.height);
        return java.util.List.of(new int[] {xo, yo, StationRecipeBook.WIDTH, StationRecipeBook.HEIGHT}, new int[] {xo - 32, yo + 3, 35, 27});
    }

    /** this station's console layout (Legacy4J's slot sizes on normal or small screens), or null to keep the normal look */
    protected @Nullable StationLayout legacyLayout(boolean sd) {
        return null;
    }

    /** whether a slot gets Legacy4J's red warning frame */
    protected java.util.function.@Nullable BooleanSupplier slotWarning(int index) {
        return null;
    }

    /** the normal size and label positions, to go back to if the console layout can't be used */
    private int @Nullable [] normal;

    private void applyLegacyLayout() {
        this.legacy = null;
        if (this.normal == null) {
            this.normal = new int[] {this.imageWidth, this.imageHeight, this.titleLabelX, this.titleLabelY, this.inventoryLabelX, this.inventoryLabelY};
        }
        if (legacyFailed || !LegacyUi.active()) return;
        try {
            StationLayout layout = this.legacyLayout(LegacyUi.sd());
            if (layout == null) return;
            this.imageWidth = (layout.width);
            this.imageHeight = (layout.height);
            this.titleLabelX = layout.titleX;
            this.titleLabelY = layout.titleY;
            this.inventoryLabelX = layout.invLabelX;
            this.inventoryLabelY = layout.invLabelY;
            for (var e : layout.slots.entrySet()) {
                int[] p = e.getValue();
                LegacyUi.slot(this.menu.slots.get(e.getKey()), p[0], p[1], p[2], this.slotWarning(e.getKey()));
            }
            this.legacy = layout;
        } catch (RuntimeException | LinkageError e) {
            legacyFailed = true;
            edu.chemistry.EduMod.LOG.warn("Couldn't use the console layout for {}", this.getClass().getSimpleName(), e);
            this.imageWidth = (this.normal[0]);
            this.imageHeight = (this.normal[1]);
            this.titleLabelX = this.normal[2];
            this.titleLabelY = this.normal[3];
            this.inventoryLabelX = this.normal[4];
            this.inventoryLabelY = this.normal[5];
        }
    }

    @Override
    protected void init() {
        this.applyLegacyLayout();
        super.init();
        StationRecipeBook book = this.book();
        if (book == null) return;
        if (book.isOpen()) {
            // same place the crafting table moves to when its recipe book is open
            this.leftPos = book.shiftedLeftPos(this.width, this.imageWidth);
        }
        int[] p = this.recipeButtonPos();
        this.addRenderableWidget(new net.minecraft.client.gui.components.ImageButton(this.leftPos + p[0], this.topPos + p[1], 20, 18,
            StationRecipeBook.buttonSprites(), b -> {
                book.toggle();
                this.rebuildWidgets();
            }));
        if (book.isOpen()) {
            this.addRenderableWidget(book.createSearchBox(this.font, this.width, this.height));
        } else {
            book.forgetSearchBox();
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float a, int mouseX, int mouseY) {
        GuiCompat.blend();
        if (this.legacy != null) LegacyUi.panel(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);
        else graphics.blit(this.texture, this.leftPos, this.topPos, (float) (0.0F), (float) (0.0F), this.imageWidth, this.imageHeight, 256, 256);
        StationRecipeBook book = this.book();
        if (book != null && book.isOpen()) {
            book.render(graphics, this.font, this.width, this.height, mouseX, mouseY);
            book.renderGhosts(graphics, this.font, this.leftPos, this.topPos, this.gridSlots(), mouseX, mouseY);
        }
    }

    /** 1.21.1's container screens draw the hovered slot's tooltip themselves */
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float a) {
        super.render(graphics, mouseX, mouseY, a);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        StationRecipeBook book = this.book();
        if (book != null && book.isOpen()
            && book.click(mx, my, this.width, this.height, this.menu.containerId, this.gridSlots(), this.resultSlot())) {
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mods) {
        StationRecipeBook book = this.book();
        net.minecraft.client.gui.components.EditBox search = book != null && book.isOpen() ? book.searchBox() : null;
        if (search != null && search.isFocused() && key != 256) {
            // typing in the search box shouldn't close the table (E) or swap hotbar items (1-9)
            search.keyPressed(key, scan, mods);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int left, int top, int button) {
        StationRecipeBook book = this.book();
        if (book != null && book.isOpen() && book.contains(mx, my, this.width, this.height)) return false;
        return super.hasClickedOutside(mx, my, left, top, button);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        this.labels(() -> {
            this.drawTitle(graphics);
            graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, this.labelColor(), false);
        });
    }

    /** draws the menu's text: as is, or in Legacy4J's font for the console layout */
    protected void labels(Runnable draw) {
        if (this.legacy != null) LegacyUi.labels(sd -> draw.run());
        else draw.run();
    }

    /** title and label colour: Java's dark gray, or Legacy4J's */
    protected int labelColor() {
        return this.legacy != null ? LegacyUi.grayText() : TITLE_COLOR;
    }

    /** Title centred over the panel, flat (no shadow) like vanilla Java container titles; left-aligned in the console layout. */
    protected void drawTitle(GuiGraphics graphics) {
        int x = this.legacy != null ? this.titleLabelX : (this.imageWidth - this.font.width(this.title)) / 2;
        graphics.drawString(this.font, this.title, x, this.titleLabelY, this.labelColor(), false);
    }

    /** draws one of the station's own pictures, shrunk to fit a smaller space in the console layout */
    protected static void scaledBlit(GuiGraphics graphics, ResourceLocation tex, int x, int y, float u, float v, int w, int h, int texW, int texH, float scale) {
        graphics.pose().pushPose();
        graphics.pose().translate(x, y, 0.0F);
        graphics.pose().scale(scale, scale, 1.0F);
        graphics.blit(tex, 0, 0, (float) (u), (float) (v), w, h, texW, texH);
        graphics.pose().popPose();
    }

    protected void blit(GuiGraphics graphics, int x, int y, int u, int v, int w, int h) {
        graphics.blit(this.texture, x, y, (float) ((float) u), (float) ((float) v), w, h, 256, 256);
    }

    protected void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }
}
