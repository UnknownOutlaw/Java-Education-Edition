package edu.chemistry.client;

import edu.chemistry.EduMod;
import edu.chemistry.registry.EduTabs;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** The side tabs shown on the left of the creative inventory while the Education Edition tab is open. */
public final class CreativeCategories {
    /** the side tabs are vanilla's creative tabs turned on their side: 32 x 26, one every 27 pixels */
    public static final int TAB_W = 32;
    public static final int TAB_H = 26;
    public static final int TAB_STEP = 27;
    /** how far a tab reaches into the inventory panel (only the open one is drawn over it) */
    private static final int OVERLAP = 4;
    private static final Identifier SELECTED_FIRST = EduMod.id("textures/gui/sidetabs/selected_first.png");
    private static final Identifier SELECTED = EduMod.id("textures/gui/sidetabs/selected.png");
    private static final Identifier UNSELECTED_FIRST = EduMod.id("textures/gui/sidetabs/unselected_first.png");
    private static final Identifier UNSELECTED = EduMod.id("textures/gui/sidetabs/unselected.png");
    /** -1 = everything */
    private static int selected = 0;

    public static int selected() {
        return selected;
    }

    static int tabX(int leftPos) {
        return leftPos - TAB_W + OVERLAP;
    }

    static int tabY(int topPos, int i) {
        return topPos + i * TAB_STEP;
    }

    /** Fills the creative grid with the selected category. */
    public static void fill(NonNullList<ItemStack> items) {
        items.clear();
        List<EduTabs.Category> cats = EduTabs.CATEGORIES;
        List<Item> source = selected >= 0 && selected < cats.size() ? cats.get(selected).items().get() : EduTabs.allItems();
        for (Item item : source) items.add(new ItemStack(item));
    }

    /** where the side tabs were last drawn, as {x, y, width, height}, or null when they're not showing (for JEI) */
    public static int @org.jspecify.annotations.Nullable [] shownArea;

    public static void render(GuiGraphicsExtractor g, int leftPos, int topPos, int mouseX, int mouseY) {
        shownArea = new int[] {tabX(leftPos), tabY(topPos, 0), TAB_W - OVERLAP, EduTabs.CATEGORIES.size() * TAB_STEP};
        List<EduTabs.Category> cats = EduTabs.CATEGORIES;
        for (int i = 0; i < cats.size(); i++) {
            int x = tabX(leftPos);
            int y = tabY(topPos, i);
            boolean sel = i == selected;
            boolean hover = mouseX >= x && mouseX < x + TAB_W - OVERLAP && mouseY >= y && mouseY < y + TAB_H;
            // like vanilla's top tabs: the open tab joins the panel, the others stop at its edge
            Identifier tex = sel ? (i == 0 ? SELECTED_FIRST : SELECTED) : (i == 0 ? UNSELECTED_FIRST : UNSELECTED);
            int w = sel ? TAB_W : TAB_W - OVERLAP;
            g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 0.0F, 0.0F, w, TAB_H, TAB_W, TAB_H);
            g.item(new ItemStack(cats.get(i).icon().get()), x + 9, y + 5);
            if (hover) g.setTooltipForNextFrame(Minecraft.getInstance().font, cats.get(i).title(), mouseX, mouseY);
        }
    }

    /** @return true if a side tab was clicked (the caller refreshes the grid) */
    public static boolean click(int leftPos, int topPos, double mouseX, double mouseY) {
        List<EduTabs.Category> cats = EduTabs.CATEGORIES;
        for (int i = 0; i < cats.size(); i++) {
            int x = tabX(leftPos);
            int y = tabY(topPos, i);
            if (mouseX >= x && mouseX < x + TAB_W - OVERLAP && mouseY >= y && mouseY < y + TAB_H) {
                selected = i;
                return true;
            }
        }
        return false;
    }

    private CreativeCategories() {
    }
}
