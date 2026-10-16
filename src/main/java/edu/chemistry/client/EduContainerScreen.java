package edu.chemistry.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Container screen with a 256x256 background texture. */
public abstract class EduContainerScreen<T extends AbstractContainerMenu> extends AbstractContainerScreen<T> {
    /** Java's container title colour (left-aligned at 8,6 like vanilla screens) */
    public static final int TITLE_COLOR = 0xFF404040;
    protected final Identifier texture;

    protected EduContainerScreen(T menu, Inventory inventory, Component title, Identifier texture, int width, int height) {
        super(menu, inventory, title, width, height);
        this.texture = texture;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    /** the compound creator and lab table have a recipe book; the other tables return null */
    protected StationRecipeBook recipeBook() {
        return null;
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
        StationRecipeBook book = this.recipeBook();
        if (book == null || !book.isOpen()) return java.util.List.of();
        int xo = StationRecipeBook.bookX(this.width);
        int yo = StationRecipeBook.bookY(this.height);
        return java.util.List.of(new int[] {xo, yo, StationRecipeBook.WIDTH, StationRecipeBook.HEIGHT}, new int[] {xo - 32, yo + 3, 35, 27});
    }

    @Override
    protected void init() {
        super.init();
        StationRecipeBook book = this.recipeBook();
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
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        graphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, this.leftPos, this.topPos, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        StationRecipeBook book = this.recipeBook();
        if (book != null && book.isOpen()) {
            book.render(graphics, this.font, this.width, this.height, mouseX, mouseY);
            book.renderGhosts(graphics, this.font, this.leftPos, this.topPos, this.gridSlots(), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        StationRecipeBook book = this.recipeBook();
        if (book != null && book.isOpen()
            && book.click(event.x(), event.y(), this.width, this.height, this.menu.containerId, this.gridSlots(), this.resultSlot())) {
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        StationRecipeBook book = this.recipeBook();
        net.minecraft.client.gui.components.EditBox search = book != null && book.isOpen() ? book.searchBox() : null;
        if (search != null && search.isFocused() && event.key() != 256) {
            // typing in the search box shouldn't close the table (E) or swap hotbar items (1-9)
            search.keyPressed(event);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    protected boolean hasClickedOutside(double mx, double my, int left, int top) {
        StationRecipeBook book = this.recipeBook();
        if (book != null && book.isOpen() && book.contains(mx, my, this.width, this.height)) return false;
        return super.hasClickedOutside(mx, my, left, top);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        this.drawTitle(graphics);
        graphics.text(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, TITLE_COLOR, false);
    }

    /** Title centred over the panel, flat (no shadow) like vanilla Java container titles. */
    protected void drawTitle(GuiGraphicsExtractor graphics) {
        int x = (this.imageWidth - this.font.width(this.title)) / 2;
        graphics.text(this.font, this.title, x, this.titleLabelY, TITLE_COLOR, false);
    }

    protected void blit(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int w, int h) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, this.texture, x, y, (float) u, (float) v, w, h, 256, 256);
    }

    protected void sendButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }
}
