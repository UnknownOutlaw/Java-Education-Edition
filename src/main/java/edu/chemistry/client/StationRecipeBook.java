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
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.WidgetSprites;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeCollection;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.display.RecipeDisplayEntry;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
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
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("textures/gui/recipe_book.png");
    private static final Identifier SLOT_CRAFTABLE = Identifier.withDefaultNamespace("recipe_book/slot_craftable");
    private static final Identifier SLOT_UNCRAFTABLE = Identifier.withDefaultNamespace("recipe_book/slot_uncraftable");
    private static final WidgetSprites PAGE_FORWARD = new WidgetSprites(Identifier.withDefaultNamespace("recipe_book/page_forward"),
        Identifier.withDefaultNamespace("recipe_book/page_forward_highlighted"));
    private static final WidgetSprites PAGE_BACK = new WidgetSprites(Identifier.withDefaultNamespace("recipe_book/page_backward"),
        Identifier.withDefaultNamespace("recipe_book/page_backward_highlighted"));
    /** same textures as the crafting table's "showing craftable" toggle */
    private static final WidgetSprites FILTER = new WidgetSprites(Identifier.withDefaultNamespace("recipe_book/filter_enabled"),
        Identifier.withDefaultNamespace("recipe_book/filter_disabled"), Identifier.withDefaultNamespace("recipe_book/filter_enabled_highlighted"),
        Identifier.withDefaultNamespace("recipe_book/filter_disabled_highlighted"));
    private static final WidgetSprites TAB = new WidgetSprites(Identifier.withDefaultNamespace("recipe_book/tab"),
        Identifier.withDefaultNamespace("recipe_book/tab_selected"));
    private static final Component SEARCH_HINT = Component.translatable("gui.recipebook.search_hint").withStyle(EditBox.SEARCH_HINT_STYLE);
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

    private List<RecipeDisplayEntry> entries(Inventory inv) {
        LocalPlayer player = Minecraft.getInstance().player;
        List<RecipeDisplayEntry> out = new ArrayList<>();
        if (player == null) return out;
        String q = this.lastSearch.trim().toLowerCase(Locale.ROOT);
        for (RecipeCollection c : player.getRecipeBook().getCollection(this.station.category)) {
            for (RecipeDisplayEntry e : c.getRecipes()) {
                if (this.filtering() && !canMake(inv, e)) continue;
                if (!q.isEmpty() && !result(e).getHoverName().getString().toLowerCase(Locale.ROOT).contains(q)) continue;
                out.add(e);
            }
        }
        return out;
    }

    private static List<ItemStackTemplate> ingredients(RecipeDisplayEntry e) {
        List<ItemStackTemplate> out = new ArrayList<>();
        if (e.display() instanceof ShapelessCraftingRecipeDisplay d) {
            for (SlotDisplay s : d.ingredients()) if (s instanceof SlotDisplay.ItemStackSlotDisplay i) out.add(i.stack());
        }
        return out;
    }

    private static ItemStack result(RecipeDisplayEntry e) {
        if (e.display() instanceof ShapelessCraftingRecipeDisplay d && d.result() instanceof SlotDisplay.ItemStackSlotDisplay i) {
            return i.stack().create();
        }
        return ItemStack.EMPTY;
    }

    private static int have(Inventory inv, ItemStackTemplate t) {
        ItemStack wanted = t.create();
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && ItemStack.isSameItemSameComponents(s, wanted)) n += s.getCount();
        }
        return n;
    }

    private static boolean canMake(Inventory inv, RecipeDisplayEntry e) {
        for (ItemStackTemplate t : ingredients(e)) if (have(inv, t) < t.count()) return false;
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

    public void render(GuiGraphicsExtractor g, Font font, int screenWidth, int screenHeight, int mouseX, int mouseY) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        int xo = bookX(screenWidth);
        int yo = bookY(screenHeight);
        // the one tab (selected), with the table as its icon
        g.blitSprite(RenderPipelines.GUI_TEXTURED, TAB.get(true, true), xo - 30 - 2, yo + 3, 35, 27);
        g.fakeItem(new ItemStack(this.station.block.get()), xo - 30 + 9 - 2, yo + 3 + 5);
        g.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, xo, yo, 1.0F, 1.0F, WIDTH, HEIGHT, 256, 256);
        // filter toggle
        boolean filterHover = in(mouseX, mouseY, xo + 110, yo + 12, 26, 16);
        g.blitSprite(RenderPipelines.GUI_TEXTURED, FILTER.get(this.filtering(), filterHover), xo + 110, yo + 12, 26, 16);
        if (filterHover) g.setTooltipForNextFrame(font, this.filtering() ? ONLY_CRAFTABLES : ALL_RECIPES, mouseX, mouseY);

        List<RecipeDisplayEntry> list = this.entries(player.getInventory());
        int pages = this.pages(list.size());
        this.page = Math.min(this.page, pages - 1);
        for (int i = 0; i < COLS * ROWS; i++) {
            int idx = this.page * COLS * ROWS + i;
            if (idx >= list.size()) break;
            RecipeDisplayEntry e = list.get(idx);
            int sx = xo + 11 + (i % COLS) * 25;
            int sy = yo + 31 + (i / COLS) * 25;
            g.blitSprite(RenderPipelines.GUI_TEXTURED, canMake(player.getInventory(), e) ? SLOT_CRAFTABLE : SLOT_UNCRAFTABLE, sx, sy, 25, 25);
            ItemStack res = result(e);
            g.fakeItem(res, sx + 4, sy + 4);
            if (in(mouseX, mouseY, sx, sy, 25, 25)) g.setTooltipForNextFrame(font, res, mouseX, mouseY);
        }
        if (pages > 1) {
            if (this.page > 0) {
                g.blitSprite(RenderPipelines.GUI_TEXTURED, PAGE_BACK.get(true, in(mouseX, mouseY, xo + 38, yo + 137, 12, 17)), xo + 38, yo + 137, 12, 17);
            }
            if (this.page < pages - 1) {
                g.blitSprite(RenderPipelines.GUI_TEXTURED, PAGE_FORWARD.get(true, in(mouseX, mouseY, xo + 93, yo + 137, 12, 17)), xo + 93, yo + 137, 12, 17);
            }
            Component text = Component.translatable("gui.recipebook.page", this.page + 1, pages);
            g.text(font, text, xo + 73 - font.width(text) / 2, yo + 141, -1, false);
        }
    }

    /** Ghost ingredients in the table, drawn like vanilla's (call after the table background). */
    public void renderGhosts(GuiGraphicsExtractor g, Font font, int leftPos, int topPos, List<Slot> grid, int mouseX, int mouseY) {
        this.renderGhosts(g, font, leftPos, topPos, grid, mouseX, mouseY, 16, 16);
    }

    /**
     * frame, resultFrame: the slot frame sizes the ghosts sit in the middle of (16 normally; Legacy4J's frames in the
     * console layout)
     */
    public void renderGhosts(GuiGraphicsExtractor g, Font font, int leftPos, int topPos, List<Slot> grid, int mouseX, int mouseY, int frame,
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
            g.fakeItem(stack, x, y);
            g.fill(x, y, x + 16, y + 16, 0x30FFFFFF);
            g.itemDecorations(font, stack, x, y);
            if (in(mouseX, mouseY, x, y, 16, 16)) g.setTooltipForNextFrame(font, stack, mouseX, mouseY);
        });
        if (this.ghostResultSlot != null && !this.ghostResult.isEmpty() && !this.ghostResultSlot.hasItem()) {
            int x = resultLeft + this.ghostResultSlot.x;
            int y = resultTop + this.ghostResultSlot.y;
            g.fill(x, y, x + 16, y + 16, 0x30FF0000);
            g.fakeItem(this.ghostResult, x, y);
            g.fill(x, y, x + 16, y + 16, 0x30FFFFFF);
            g.itemDecorations(font, this.ghostResult, x, y);
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
        List<RecipeDisplayEntry> list = this.entries(player.getInventory());
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
            RecipeDisplayEntry e = list.get(idx);
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode == null) return true;
            // the server takes out what's in the table, then fills it with whatever you have of the recipe
            mc.gameMode.handleInventoryButtonClick(containerId, StationPlacement.PLACE_BASE + e.id().index());
            this.clearGhosts();
            if (!canMake(player.getInventory(), e)) {
                // vanilla shows what's missing as ghost items in the grid
                List<ItemStackTemplate> ings = ingredients(e);
                for (int k = 0; k < ings.size() && k < grid.size(); k++) this.ghosts.put(grid.get(k), ings.get(k).create());
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
