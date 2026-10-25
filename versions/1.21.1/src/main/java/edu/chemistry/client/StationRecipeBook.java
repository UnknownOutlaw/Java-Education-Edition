package edu.chemistry.client;

import edu.chemistry.recipe.StationPlacement;
import edu.chemistry.recipe.StationRecipe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jspecify.annotations.Nullable;

/**
 * The compound creator / lab table recipe book, laid out exactly like the crafting table's: the book beside the table
 * (the table slides right), a search box with the magnifier, the "showing craftable / all" filter, a tab on the left,
 * five by four recipe buttons and the page arrows. Clicking a recipe you can make moves the ingredients in; clicking
 * one you can't make shows ghost ingredients in the table, like vanilla.
 */
public class StationRecipeBook {
    public static final int WIDTH = 147;
    public static final int HEIGHT = 166;
    private static final int COLS = 5;
    private static final int ROWS = 4;
    private static final ResourceLocation BACKGROUND = ResourceLocation.withDefaultNamespace("textures/gui/recipe_book.png");
    private static final ResourceLocation SLOT_CRAFTABLE = ResourceLocation.withDefaultNamespace("recipe_book/slot_craftable");
    private static final ResourceLocation SLOT_UNCRAFTABLE = ResourceLocation.withDefaultNamespace("recipe_book/slot_uncraftable");
    private static final WidgetSprites PAGE_FORWARD = new WidgetSprites(ResourceLocation.withDefaultNamespace("recipe_book/page_forward"),
        ResourceLocation.withDefaultNamespace("recipe_book/page_forward_highlighted"));
    private static final WidgetSprites PAGE_BACK = new WidgetSprites(ResourceLocation.withDefaultNamespace("recipe_book/page_backward"),
        ResourceLocation.withDefaultNamespace("recipe_book/page_backward_highlighted"));
    /** same textures as the crafting table's "showing craftable" toggle */
    private static final WidgetSprites FILTER = new WidgetSprites(ResourceLocation.withDefaultNamespace("recipe_book/filter_enabled"),
        ResourceLocation.withDefaultNamespace("recipe_book/filter_disabled"), ResourceLocation.withDefaultNamespace("recipe_book/filter_enabled_highlighted"),
        ResourceLocation.withDefaultNamespace("recipe_book/filter_disabled_highlighted"));
    private static final WidgetSprites TAB = new WidgetSprites(ResourceLocation.withDefaultNamespace("recipe_book/tab"),
        ResourceLocation.withDefaultNamespace("recipe_book/tab_selected"));
    private static final Component SEARCH_HINT = Component.translatable("gui.recipebook.search_hint")
        .withStyle(net.minecraft.ChatFormatting.ITALIC).withStyle(net.minecraft.ChatFormatting.GRAY);
    private static final Component ONLY_CRAFTABLES = Component.translatable("gui.recipebook.toggleRecipes.craftable");
    private static final Component ALL_RECIPES = Component.translatable("gui.recipebook.toggleRecipes.all");

    /** open / filtering are remembered per table, like the vanilla book */
    private static final Map<String, Boolean> OPEN = new HashMap<>();
    private static final Map<String, Boolean> FILTERING = new HashMap<>();

    private final StationRecipe.Station station;
    private int page;
    private @Nullable EditBox search;
    private String lastSearch = "";
    /** ghost ingredients shown in the table after clicking a recipe you can't make: slot -> item */
    private final Map<Slot, ItemStack> ghosts = new HashMap<>();
    private @Nullable Slot ghostResultSlot;
    private ItemStack ghostResult = ItemStack.EMPTY;

    public StationRecipeBook(StationRecipe.Station station) {
        this.station = station;
    }

    public boolean isOpen() {
        return OPEN.getOrDefault(this.station.name, false);
    }

    public void toggle() {
        OPEN.put(this.station.name, !this.isOpen());
        this.clearGhosts();
    }

    private boolean filtering() {
        return FILTERING.getOrDefault(this.station.name, false);
    }

    private static boolean tooNarrow(int width) {
        return width < 379;
    }

    /** the crafting table's formula for where the table sits while the book is open */
    public int shiftedLeftPos(int screenWidth, int imageWidth) {
        return tooNarrow(screenWidth) ? (screenWidth - imageWidth) / 2 : 177 + (screenWidth - imageWidth - 200) / 2;
    }

    public static int bookX(int screenWidth) {
        return (screenWidth - WIDTH) / 2 - (tooNarrow(screenWidth) ? 0 : 86);
    }

    public static int bookY(int screenHeight) {
        return (screenHeight - HEIGHT) / 2;
    }

    /** Builds the search box (call from the screen's init while the book is open). */
    public EditBox createSearchBox(Font font, int screenWidth, int screenHeight) {
        int xo = bookX(screenWidth);
        int yo = bookY(screenHeight);
        EditBox box = new EditBox(font, xo + 25, yo + 13, 81, 9 + 5, Component.translatable("itemGroup.search"));
        box.setMaxLength(50);
        box.setTextColor(-1);
        box.setValue(this.lastSearch);
        box.setHint(SEARCH_HINT);
        box.setResponder(s -> {
            if (!s.equals(this.lastSearch)) {
                this.lastSearch = s;
                this.page = 0;
            }
        });
        this.search = box;
        return box;
    }

    public void forgetSearchBox() {
        this.search = null;
    }

    public @Nullable EditBox searchBox() {
        return this.search;
    }

    /** a recipe in the book: its place in the table's sorted recipe list (what the server is sent) and the recipe */
    private record Entry(int index, RecipeHolder<StationRecipe> holder) {
    }

    /** the table's recipes you've unlocked (1.21.1 sends every recipe to the client, and which ones you know) */
    private List<Entry> entries(Inventory inv) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        List<Entry> out = new ArrayList<>();
        if (player == null || mc.level == null) return out;
        String q = this.lastSearch.trim().toLowerCase(Locale.ROOT);
        List<RecipeHolder<StationRecipe>> all = StationRecipe.all(mc.level.getRecipeManager(), this.station);
        for (int i = 0; i < all.size(); i++) {
            Entry e = new Entry(i, all.get(i));
            if (!player.getRecipeBook().contains(e.holder())) continue;
            if (this.filtering() && !canMake(inv, e)) continue;
            if (!q.isEmpty() && !result(e).getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)) continue;
            out.add(e);
        }
        return out;
    }

    private static List<ItemStack> ingredients(Entry e) {
        return e.holder().value().ingredients();
    }

    private static ItemStack result(Entry e) {
        return e.holder().value().result().copy();
    }

    private static int have(Inventory inv, ItemStack wanted) {
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, wanted)) n += s.getCount();
        }
        return n;
    }

    private static boolean canMake(Inventory inv, Entry e) {
        for (ItemStack t : ingredients(e)) if (have(inv, t) < t.getCount()) return false;
        return true;
    }

    private int pages(int count) {
        return Math.max(1, (count + COLS * ROWS - 1) / (COLS * ROWS));
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** the area the book covers, including its tab */
    public boolean contains(double mx, double my, int screenWidth, int screenHeight) {
        int xo = bookX(screenWidth);
        int yo = bookY(screenHeight);
        return in(mx, my, xo, yo, WIDTH, HEIGHT) || in(mx, my, xo - 32, yo + 3, 35, 27);
    }

    public void render(GuiGraphics g, Font font, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        GuiCompat.blend();
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        int xo = bookX(screenWidth);
        int yo = bookY(screenHeight);
        // the one tab (selected), with the table as its icon
        g.blitSprite(TAB.get(true, true), xo - 30 - 2, yo + 3, 35, 27);
        g.renderFakeItem(new ItemStack(this.station.block.get()), xo - 30 + 9 - 2, yo + 3 + 5);
        g.blit(BACKGROUND, xo, yo, (float) (1.0F), (float) (1.0F), WIDTH, HEIGHT, 256, 256);
        // filter toggle
        boolean filterHover = in(mouseX, mouseY, xo + 110, yo + 12, 26, 16);
        g.blitSprite(FILTER.get(this.filtering(), filterHover), xo + 110, yo + 12, 26, 16);
        if (filterHover) GuiCompat.tooltip(g, font, this.filtering() ? ONLY_CRAFTABLES : ALL_RECIPES, mouseX, mouseY);

        List<Entry> list = this.entries(player.getInventory());
        int pages = this.pages(list.size());
        this.page = Math.min(this.page, pages - 1);
        for (int i = 0; i < COLS * ROWS; i++) {
            int idx = this.page * COLS * ROWS + i;
            if (idx >= list.size()) break;
            Entry e = list.get(idx);
            int sx = xo + 11 + (i % COLS) * 25;
            int sy = yo + 31 + (i / COLS) * 25;
            g.blitSprite(canMake(player.getInventory(), e) ? SLOT_CRAFTABLE : SLOT_UNCRAFTABLE, sx, sy, 25, 25);
            ItemStack res = result(e);
            g.renderFakeItem(res, sx + 4, sy + 4);
            if (in(mouseX, mouseY, sx, sy, 25, 25)) GuiCompat.tooltip(g, font, res, mouseX, mouseY);
        }
        if (pages > 1) {
            if (this.page > 0) {
                g.blitSprite(PAGE_BACK.get(true, in(mouseX, mouseY, xo + 38, yo + 137, 12, 17)), xo + 38, yo + 137, 12, 17);
            }
            if (this.page < pages - 1) {
                g.blitSprite(PAGE_FORWARD.get(true, in(mouseX, mouseY, xo + 93, yo + 137, 12, 17)), xo + 93, yo + 137, 12, 17);
            }
            Component text = Component.translatable("gui.recipebook.page", this.page + 1, pages);
            g.drawString(font, text, xo + 73 - font.width(text) / 2, yo + 141, -1, false);
        }
    }

    /** Ghost ingredients in the table, drawn like vanilla's (call after the table background). */
    public void renderGhosts(GuiGraphics g, Font font, int leftPos, int topPos, List<Slot> grid, int mouseX, int mouseY) {
        this.renderGhosts(g, font, leftPos, topPos, grid, mouseX, mouseY, 16, 16);
    }

    /**
     * frame, resultFrame: the slot frame sizes the ghosts sit in the middle of (16 normally; Legacy4J's frames in the
     * console layout)
     */
    public void renderGhosts(GuiGraphics g, Font font, int leftPos, int topPos, List<Slot> grid, int mouseX, int mouseY, int frame,
                             int resultFrame) {
        int resultInset = (resultFrame - 16) / 2;
        int resultLeft = leftPos + resultInset;
        int resultTop = topPos + resultInset;
        int inset = (frame - 16) / 2;
        int gridLeft = leftPos + inset;
        int gridTop = topPos + inset;
        for (Slot s : grid) {
            if (s.hasItem()) {
                this.clearGhosts();
                return;
            }
        }
        this.ghosts.forEach((slot, stack) -> {
            int x = gridLeft + slot.x;
            int y = gridTop + slot.y;
            g.fill(x, y, x + 16, y + 16, 0x30FF0000);
            g.renderFakeItem(stack, x, y);
            g.fill(net.minecraft.client.renderer.RenderType.guiGhostRecipeOverlay(), x, y, x + 16, y + 16, 0x30FFFFFF);
            g.renderItemDecorations(font, stack, x, y);
            if (in(mouseX, mouseY, x, y, 16, 16)) GuiCompat.tooltip(g, font, stack, mouseX, mouseY);
        });
        if (this.ghostResultSlot != null && !this.ghostResult.isEmpty() && !this.ghostResultSlot.hasItem()) {
            int x = resultLeft + this.ghostResultSlot.x;
            int y = resultTop + this.ghostResultSlot.y;
            g.fill(x, y, x + 16, y + 16, 0x30FF0000);
            g.renderFakeItem(this.ghostResult, x, y);
            g.fill(net.minecraft.client.renderer.RenderType.guiGhostRecipeOverlay(), x, y, x + 16, y + 16, 0x30FFFFFF);
            g.renderItemDecorations(font, this.ghostResult, x, y);
        }
    }

    private void clearGhosts() {
        this.ghosts.clear();
        this.ghostResultSlot = null;
        this.ghostResult = ItemStack.EMPTY;
    }

    /** @return true if the click was on the book */
    public boolean click(double mx, double my, int screenWidth, int screenHeight, int containerId, List<Slot> grid, @Nullable Slot resultSlot) {
        if (!this.contains(mx, my, screenWidth, screenHeight)) return false;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return true;
        int xo = bookX(screenWidth);
        int yo = bookY(screenHeight);
        if (this.search != null && in(mx, my, xo + 8, yo + 13, 98, 14)) {
            this.search.setFocused(true);
            return false; // let the screen focus and position the cursor in the box
        }
        if (this.search != null) this.search.setFocused(false);
        if (in(mx, my, xo + 110, yo + 12, 26, 16)) {
            FILTERING.put(this.station.name, !this.filtering());
            this.page = 0;
            return true;
        }
        List<Entry> list = this.entries(player.getInventory());
        int pages = this.pages(list.size());
        if (this.page > 0 && in(mx, my, xo + 38, yo + 137, 12, 17)) {
            this.page--;
            return true;
        }
        if (this.page < pages - 1 && in(mx, my, xo + 93, yo + 137, 12, 17)) {
            this.page++;
            return true;
        }
        for (int i = 0; i < COLS * ROWS; i++) {
            int idx = this.page * COLS * ROWS + i;
            if (idx >= list.size()) break;
            if (!in(mx, my, xo + 11 + (i % COLS) * 25, yo + 31 + (i / COLS) * 25, 25, 25)) continue;
            Entry e = list.get(idx);
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode == null) return true;
            // the server takes out what's in the table, then fills it with whatever you have of the recipe
            mc.gameMode.handleInventoryButtonClick(containerId, StationPlacement.PLACE_BASE + e.index());
            this.clearGhosts();
            if (!canMake(player.getInventory(), e)) {
                // vanilla shows what's missing as ghost items in the grid
                List<ItemStack> ings = ingredients(e);
                for (int k = 0; k < ings.size() && k < grid.size(); k++) this.ghosts.put(grid.get(k), ings.get(k).copy());
                this.ghostResultSlot = resultSlot;
                this.ghostResult = result(e);
            }
            return true;
        }
        return true;
    }

    public static WidgetSprites buttonSprites() {
        return RecipeBookComponent.RECIPE_BUTTON_SPRITES;
    }
}
