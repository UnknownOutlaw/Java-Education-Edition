package edu.chemistry.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.chemistry.EduMod;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.PageButton;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import org.jspecify.annotations.Nullable;

/**
 * The Chemistry Guide: an open book with two pages side by side, coloured bookmarks down the side for its chapters,
 * and every recipe drawn with the real items (hover one to see its name). Its pages come from
 * assets/edu/guide/pages.json, generated from the mod's own recipes.
 */
public class GuideBookScreen extends Screen {
    private static final Identifier BOOK = EduMod.id("textures/gui/guide/book.png");
    private static final Identifier SLOT = Identifier.withDefaultNamespace("container/slot");
    private static final Identifier ARROW_TEXTURE = Identifier.withDefaultNamespace("textures/gui/container/crafting_table.png");
    private static final int BOOK_W = 296;
    private static final int BOOK_H = 184;
    private static final int PAGE_W = 116;
    /** where the two pages' writing starts, from the book's left edge */
    private static final int LEFT_PAGE_X = 20;
    private static final int RIGHT_PAGE_X = 160;
    private static final int CONTENT_TOP = 12;
    /** the lowest a line of notes can start, from the book's top */
    private static final int TEXT_BOTTOM = 147;
    private static final int TEXT = 0xFF2B2118;
    private static final int SOFT = 0xFF7A6650;
    private static final int TEAL = 0xFF1F6B64;
    private static final int TAB_H = 20;
    private static final int TAB_STEP = 22;
    /** where you were (the left page of the open spread), so the book opens on the same pages next time */
    private static int lastSpread;

    private record Slot(int x, int y, ItemStack stack) {
    }

    private record Chapter(String label, int page, ItemStack icon, int color) {
    }

    private record Page(String kind, String title, @Nullable String subtitle, @Nullable ItemStack station, List<@Nullable ItemStack> inputs,
                        List<ItemStack> outputs, boolean grid, String text, boolean chapter, @Nullable ItemStack icon,
                        List<ItemStack> show, int section) {
    }

    /** one page as shown: a page of the guide, the rest of its notes when they didn't all fit, or a blank page */
    private record View(@Nullable Page page, boolean continued, int firstLine, int lastLine) {
    }

    private final List<Chapter> chapters = new ArrayList<>();
    /** the big item under the mouse this frame, if any */
    private @Nullable ItemStack hoveredShow;
    private final List<Page> pages = new ArrayList<>();
    private final List<View> views = new ArrayList<>();
    /** guide page -> the view it starts on */
    private final List<Integer> startOf = new ArrayList<>();
    /** the view on the left of the open spread (always even) */
    private int spread;
    private PageButton forward;
    private PageButton back;

    public GuideBookScreen() {
        super(Component.translatable("item.edu.guide_book"));
        this.load();
        this.paginate();
        this.spread = this.clampSpread(lastSpread);
    }

    // ---------------------------------------------------------------- loading

    private void load() {
        Optional<Resource> res = Minecraft.getInstance().getResourceManager().getResource(EduMod.id("guide/pages.json"));
        if (res.isEmpty()) return;
        try (Reader reader = res.get().openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("chapters")) {
                for (JsonElement e : root.getAsJsonArray("chapters")) {
                    JsonObject c = e.getAsJsonObject();
                    this.chapters.add(new Chapter(c.get("label").getAsString(), c.get("page").getAsInt(), item(c.get("icon").getAsString(), 1),
                        (int) c.get("color").getAsLong()));
                }
            }
            JsonArray arr = root.getAsJsonArray("pages");
            for (JsonElement e : arr) {
                JsonObject o = e.getAsJsonObject();
                List<@Nullable ItemStack> inputs = new ArrayList<>();
                if (o.has("inputs")) for (JsonElement c : o.getAsJsonArray("inputs")) inputs.add(c.isJsonNull() ? null : stack(c.getAsJsonObject()));
                List<ItemStack> outputs = new ArrayList<>();
                if (o.has("outputs")) for (JsonElement c : o.getAsJsonArray("outputs")) outputs.add(stack(c.getAsJsonObject()));
                this.pages.add(new Page(o.has("kind") ? o.get("kind").getAsString() : "page", o.get("title").getAsString(),
                    o.has("subtitle") ? o.get("subtitle").getAsString() : null,
                    o.has("station") ? item(o.get("station").getAsString(), 1) : null, inputs, outputs,
                    o.has("grid") && o.get("grid").getAsBoolean(), o.has("text") ? o.get("text").getAsString() : "",
                    o.has("chapter") && o.get("chapter").getAsBoolean(), o.has("icon") ? item(o.get("icon").getAsString(), 1) : null,
                    shows(o), o.has("section") ? o.get("section").getAsInt() : -1));
            }
        } catch (Exception e) {
            EduMod.LOG.warn("Couldn't read the Chemistry Guide", e);
        }
    }

    private static List<ItemStack> shows(JsonObject o) {
        List<ItemStack> out = new ArrayList<>();
        if (!o.has("show")) return out;
        JsonElement e = o.get("show");
        if (e.isJsonArray()) for (JsonElement c : e.getAsJsonArray()) out.add(stack(c.getAsJsonObject()));
        else out.add(stack(e.getAsJsonObject()));
        return out;
    }

    private static ItemStack stack(JsonObject c) {
        ItemStack stack = item(c.get("id").getAsString(), c.has("count") ? c.get("count").getAsInt() : 1);
        if (c.has("potion")) {
            BuiltInRegistries.POTION.get(Identifier.parse(c.get("potion").getAsString()))
                .ifPresent(potion -> stack.set(DataComponents.POTION_CONTENTS, new PotionContents(potion)));
        }
        return stack;
    }

    private static ItemStack item(String id, int count) {
        return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(id)), count);
    }

    /** Lays the guide out on pages: chapters always start on a left-hand page, and long notes carry on overleaf. */
    private void paginate() {
        Font font = Minecraft.getInstance().font;
        for (Page p : this.pages) {
            if (p.chapter() && this.views.size() % 2 == 1) this.views.add(new View(null, false, 0, 0));
            this.startOf.add(this.views.size());
            int lines = p.text().isEmpty() ? 0 : font.split(Component.literal(p.text()), PAGE_W).size();
            int fit = Math.max(0, (TEXT_BOTTOM - this.layout(null, font, new View(p, false, 0, 0), 0, 0, 0, 0, null)) / 9 + 1);
            int shown = Math.min(lines, fit);
            this.views.add(new View(p, false, 0, shown));
            int contFit = Math.max(1, (TEXT_BOTTOM - this.layout(null, font, new View(p, true, 0, 0), 0, 0, 0, 0, null)) / 9 + 1);
            while (shown < lines) {
                int next = Math.min(lines, shown + contFit);
                this.views.add(new View(p, true, shown, next));
                shown = next;
            }
        }
        if (this.views.size() % 2 == 1) this.views.add(new View(null, false, 0, 0));
    }

    // ---------------------------------------------------------------- placement and navigation

    private int bookX() {
        return (this.width - BOOK_W) / 2 - 10;
    }

    private int bookY() {
        return Math.max(2, (this.height - BOOK_H - 24) / 2);
    }

    private int clampSpread(int s) {
        return Math.max(0, Math.min(s, this.views.size() - 2)) & ~1;
    }

    @Override
    protected void init() {
        int bx = this.bookX();
        int by = this.bookY();
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
            .pos((this.width - 200) / 2, by + BOOK_H + 4).width(200).build());
        this.back = this.addRenderableWidget(new PageButton(bx + 14, by + BOOK_H - 27, false, b -> this.turn(-2), true));
        this.forward = this.addRenderableWidget(new PageButton(bx + BOOK_W - 37, by + BOOK_H - 27, true, b -> this.turn(2), true));
        this.updateButtons();
    }

    private void turn(int by) {
        this.setSpread(this.spread + by);
    }

    private void showView(int view) {
        this.setSpread(view & ~1);
    }

    private void setSpread(int s) {
        this.spread = this.clampSpread(s);
        lastSpread = this.spread;
        this.updateButtons();
    }

    private void updateButtons() {
        if (this.forward == null) return;
        this.forward.visible = this.spread + 2 < this.views.size();
        this.back.visible = this.spread > 0;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (super.keyPressed(event)) return true;
        switch (event.key()) {
            case InputConstants.KEY_PAGEUP, InputConstants.KEY_LEFT -> this.turn(-2);
            case InputConstants.KEY_PAGEDOWN, InputConstants.KEY_RIGHT -> this.turn(2);
            case InputConstants.KEY_HOME -> this.setSpread(0); // cover and contents
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0.0) {
            this.turn(scrollY > 0 ? -2 : 2);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
            int tab = this.tabAt(event.x(), event.y());
            if (tab >= 0) {
                this.showView(this.startOf.get(this.chapters.get(tab).page()));
                this.playPageSound();
                return true;
            }
            int entry = this.contentsEntryAt(event.x(), event.y());
            if (entry >= 0) {
                this.showView(this.startOf.get(this.chapters.get(entry).page()));
                this.playPageSound();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void playPageSound() {
        Minecraft.getInstance().getSoundManager().play(
            net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 1.0F));
    }

    // ---------------------------------------------------------------- bookmarks

    /** which chapter is open on the current spread */
    private int openSection() {
        for (int i = this.spread; i <= this.spread + 1 && i < this.views.size(); i++) {
            Page p = this.views.get(i).page();
            if (p != null) return p.section();
        }
        return -1;
    }

    private int tabX(int i) {
        return this.bookX() + BOOK_W - 4;
    }

    private int tabY(int i) {
        return this.bookY() + 10 + i * TAB_STEP;
    }

    private int tabW(int i) {
        return i == this.openSection() ? 26 : 22;
    }

    private int tabAt(double mx, double my) {
        for (int i = 0; i < this.chapters.size(); i++) {
            int x = this.tabX(i) + 4;
            int y = this.tabY(i);
            if (mx >= x && mx < this.tabX(i) + this.tabW(i) && my >= y && my < y + TAB_H) return i;
        }
        return -1;
    }

    private static int darker(int color, float f) {
        int r = (int) (((color >> 16) & 0xFF) * f);
        int g = (int) (((color >> 8) & 0xFF) * f);
        int b = (int) ((color & 0xFF) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int lighter(int color, float f) {
        int r = ((color >> 16) & 0xFF);
        int g = ((color >> 8) & 0xFF);
        int b = (color & 0xFF);
        r += (int) ((255 - r) * f);
        g += (int) ((255 - g) * f);
        b += (int) ((255 - b) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private void drawTabs(GuiGraphicsExtractor g) {
        for (int i = 0; i < this.chapters.size(); i++) {
            int c = this.chapters.get(i).color();
            int x0 = this.tabX(i);
            int x1 = x0 + this.tabW(i);
            int y0 = this.tabY(i);
            int y1 = y0 + TAB_H;
            g.fill(x0, y0, x1, y1, darker(c, 0.45F));
            g.fill(x0, y0 + 1, x1 - 1, y1 - 1, c);
            g.fill(x0, y0 + 1, x1 - 1, y0 + 2, lighter(c, 0.35F));
            g.fill(x1 - 2, y0 + 2, x1 - 1, y1 - 1, darker(c, 0.75F));
            g.fill(x0, y1 - 2, x1 - 1, y1 - 1, darker(c, 0.75F));
        }
    }

    // ---------------------------------------------------------------- drawing

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        // bookmarks first, so the book's cover sits over their inner ends
        this.drawTabs(g);
        g.blit(RenderPipelines.GUI_TEXTURED, BOOK, this.bookX(), this.bookY(), 0.0F, 0.0F, BOOK_W, BOOK_H, 512, 256);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        if (this.views.isEmpty()) return;
        int bx = this.bookX();
        int by = this.bookY();
        List<Slot> slots = new ArrayList<>();
        this.hoveredShow = null;
        for (int side = 0; side < 2; side++) {
            int index = this.spread + side;
            if (index >= this.views.size()) break;
            int x0 = bx + (side == 0 ? LEFT_PAGE_X : RIGHT_PAGE_X);
            View v = this.views.get(index);
            if (v.page() != null) {
                int y = this.layout(g, this.font, v, x0, by, mouseX, mouseY, slots);
                List<FormattedCharSequence> lines = v.page().text().isEmpty() ? List.of()
                    : this.font.split(Component.literal(v.page().text()), PAGE_W);
                for (int i = v.firstLine(); i < Math.min(v.lastLine(), lines.size()); i++) {
                    g.text(this.font, lines.get(i), x0, y, TEXT, false);
                    y += 9;
                }
            }
            // page number at the foot of the page
            if (index > 0) {
                String num = String.valueOf(index + 1);
                g.text(this.font, num, x0 + (PAGE_W - this.font.width(num)) / 2, by + BOOK_H - 24, SOFT, false);
            }
        }

        // bookmark icons, and the name of the one under the mouse
        int hoveredTab = this.tabAt(mouseX, mouseY);
        for (int i = 0; i < this.chapters.size(); i++) {
            g.item(this.chapters.get(i).icon(), this.tabX(i) + (this.tabW(i) - 16) / 2 + 2, this.tabY(i) + 2);
        }

        ItemStack hovered = null;
        for (Slot s : slots) {
            g.item(s.stack(), s.x(), s.y());
            g.itemDecorations(this.font, s.stack(), s.x(), s.y());
            if (mouseX >= s.x() - 1 && mouseX < s.x() + 17 && mouseY >= s.y() - 1 && mouseY < s.y() + 17) {
                hovered = s.stack();
                g.fill(s.x(), s.y(), s.x() + 16, s.y() + 16, 0x80FFFFFF);
            }
        }
        if (hovered == null) hovered = this.hoveredShow;
        if (hovered != null) {
            g.setTooltipForNextFrame(this.font, hovered, mouseX, mouseY);
        } else if (hoveredTab >= 0) {
            g.setTooltipForNextFrame(this.font, Component.literal(this.chapters.get(hoveredTab).label()), mouseX, mouseY);
        }
    }

    private void centered(@Nullable GuiGraphicsExtractor g, Font font, FormattedCharSequence line, int x0, int y, int color) {
        if (g != null) g.text(font, line, x0 + (PAGE_W - font.width(line)) / 2, y, color, false);
    }

    /** text drawn bigger, centred on the page */
    private void bigText(@Nullable GuiGraphicsExtractor g, Font font, Component text, int x0, int y, float scale, int color) {
        if (g == null) return;
        g.pose().pushMatrix();
        g.pose().translate(x0 + PAGE_W / 2.0F - font.width(text) * scale / 2.0F, y);
        g.pose().scale(scale, scale);
        g.text(font, text, 0, 0, color, false);
        g.pose().popMatrix();
    }

    /** an item drawn bigger, centred on the page */
    private void bigItem(@Nullable GuiGraphicsExtractor g, ItemStack stack, int x0, int y, float scale) {
        if (g == null) return;
        g.pose().pushMatrix();
        g.pose().translate(x0 + PAGE_W / 2.0F - 8.0F * scale, y);
        g.pose().scale(scale, scale);
        g.item(stack, 0, 0);
        g.pose().popMatrix();
    }

    private void slot(@Nullable GuiGraphicsExtractor g, int x, int y) {
        if (g != null) g.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT, x, y, 18, 18);
    }

    private int sectionColor(Page p) {
        return p.section() >= 0 && p.section() < this.chapters.size() ? this.chapters.get(p.section()).color() : TEAL;
    }

    private static final int CONTENTS_TOP = 34;
    private static final int CONTENTS_STEP = 18;

    private int contentsEntryAt(double mx, double my) {
        for (int side = 0; side < 2; side++) {
            int index = this.spread + side;
            if (index >= this.views.size()) break;
            View v = this.views.get(index);
            if (v.page() == null || v.continued() || !v.page().kind().equals("contents")) continue;
            int x0 = this.bookX() + (side == 0 ? LEFT_PAGE_X : RIGHT_PAGE_X);
            for (int i = 0; i < this.chapters.size(); i++) {
                int y = this.bookY() + CONTENTS_TOP + i * CONTENTS_STEP;
                if (mx >= x0 && mx < x0 + PAGE_W && my >= y && my < y + 16) return i;
            }
        }
        return -1;
    }

    /**
     * Draws a page's heading and recipe (or just measures it when {@code g} is null).
     * @return where the page's notes start
     */
    private int layout(@Nullable GuiGraphicsExtractor g, Font font, View v, int x0, int by, int mouseX, int mouseY,
                       @Nullable List<Slot> slots) {
        Page p = v.page();
        int y = by + CONTENT_TOP;
        if (p == null) return y;
        int color = this.sectionColor(p);

        if (p.kind().equals("cover") && !v.continued()) {
            if (p.icon() != null) this.bigItem(g, p.icon(), x0, y + 10, 3.0F);
            y += 10 + 48 + 10;
            this.bigText(g, font, Component.literal(p.title()).withStyle(ChatFormatting.BOLD), x0, y, 1.5F, TEAL);
            y += 16;
            if (g != null) {
                Component sub = Component.literal("Education Edition");
                g.text(font, sub, x0 + (PAGE_W - font.width(sub)) / 2, y, SOFT, false);
                g.fill(x0 + 18, y + 13, x0 + PAGE_W - 18, y + 14, TEAL);
            }
            return y + 22;
        }

        if (p.kind().equals("contents") && !v.continued()) {
            this.bigText(g, font, Component.literal(p.title()).withStyle(ChatFormatting.BOLD), x0, y + 2, 1.5F, TEAL);
            if (g != null) {
                g.fill(x0, y + 17, x0 + PAGE_W, y + 18, TEAL);
                int hovered = this.contentsEntryAt(mouseX, mouseY);
                for (int i = 0; i < this.chapters.size(); i++) {
                    Chapter c = this.chapters.get(i);
                    int ly = by + CONTENTS_TOP + i * CONTENTS_STEP;
                    if (i == hovered) g.fill(x0 - 2, ly - 1, x0 + PAGE_W + 2, ly + 17, (c.color() & 0x00FFFFFF) | 0x30000000);
                    g.item(c.icon(), x0, ly);
                    Component label = Component.literal(c.label());
                    if (i == hovered) label = label.copy().withStyle(ChatFormatting.UNDERLINE);
                    g.text(font, label, x0 + 20, ly + 4, darker(c.color(), 0.8F), false);
                    String num = String.valueOf(this.startOf.get(c.page()) + 1);
                    g.text(font, num, x0 + PAGE_W - font.width(num), ly + 4, SOFT, false);
                }
            }
            return by + TEXT_BOTTOM + 9;
        }

        if (p.chapter() && !v.continued()) {
            // a chapter's first page: its icon, big title and a coloured rule
            if (p.icon() != null) this.bigItem(g, p.icon(), x0, y + 4, 2.0F);
            y += 4 + 32 + 6;
            for (FormattedCharSequence line : font.split(Component.literal(p.title()).withStyle(ChatFormatting.BOLD), (int) (PAGE_W / 1.5F))) {
                if (g != null) {
                    g.pose().pushMatrix();
                    g.pose().translate(x0 + PAGE_W / 2.0F - font.width(line) * 0.75F, y);
                    g.pose().scale(1.5F, 1.5F);
                    g.text(font, line, 0, 0, darker(color, 0.85F), false);
                    g.pose().popMatrix();
                }
                y += 14;
            }
            if (g != null) {
                g.fill(x0 + 8, y + 1, x0 + PAGE_W - 8, y + 2, color);
                g.fill(x0 + PAGE_W / 2 - 2, y, x0 + PAGE_W / 2 + 2, y + 3, color);
            }
            return y + 9;
        }

        // a normal page: the chapter's name in its colour, the title, and a rule
        if (p.section() >= 0 && p.section() < this.chapters.size() && g != null) {
            String chapterName = this.chapters.get(p.section()).label().toUpperCase(java.util.Locale.ROOT);
            g.pose().pushMatrix();
            g.pose().translate(x0, y);
            g.pose().scale(0.75F, 0.75F);
            g.text(font, chapterName, 0, 0, color, false);
            g.pose().popMatrix();
        }
        y += 9;
        String titleText = v.continued() ? p.title() + " (cont.)" : p.title();
        for (FormattedCharSequence line : font.split(Component.literal(titleText).withStyle(ChatFormatting.BOLD), PAGE_W)) {
            if (g != null) g.text(font, line, x0, y, TEXT, false);
            y += 10;
        }
        if (g != null) g.fill(x0, y, x0 + PAGE_W, y + 1, color);
        y += 4;
        if (v.continued()) return y;
        if (p.subtitle() != null) {
            for (FormattedCharSequence line : font.split(Component.literal(p.subtitle()), PAGE_W)) {
                this.centered(g, font, line, x0, y, SOFT);
                y += 9;
            }
            y += 1;
        }

        if (!p.show().isEmpty()) {
            // the items the page is about, shown big in framed boxes
            y += 2;
            int n = p.show().size();
            int gap = 4;
            int sx0 = x0 + (PAGE_W - (n * 36 + (n - 1) * gap)) / 2;
            for (int i = 0; i < n; i++) {
                int sx = sx0 + i * (36 + gap);
                if (g != null) {
                    g.fill(sx - 1, y - 1, sx + 37, y + 37, darker(color, 0.6F));
                    g.fill(sx, y, sx + 36, y + 36, lighter(color, 0.75F));
                    g.pose().pushMatrix();
                    g.pose().translate(sx + 2, y + 2);
                    g.pose().scale(2.0F, 2.0F);
                    g.item(p.show().get(i), 0, 0);
                    g.pose().popMatrix();
                }
                // hovering the box names the item
                if (slots != null && mouseX >= sx && mouseX < sx + 36 && mouseY >= y && mouseY < y + 36) {
                    this.hoveredShow = p.show().get(i);
                }
            }
            return y + 36 + 6;
        }

        if (p.inputs().isEmpty() && p.outputs().isEmpty()) return y + 2;
        if (p.station() != null) {
            for (FormattedCharSequence line : font.split(Component.translatable("guide.edu.made_in", p.station().getHoverName()), PAGE_W)) {
                this.centered(g, font, line, x0, y, SOFT);
                y += 9;
            }
        }
        y += 3;
        List<@Nullable ItemStack> inputs = p.inputs();
        if (inputs.isEmpty() && p.station() != null) inputs = List.of(p.station());
        int inCols = p.grid() ? 3 : Math.min(3, Math.max(1, inputs.size()));
        int inRows = p.grid() ? 3 : (inputs.size() + inCols - 1) / inCols;
        int outCols = Math.min(3, Math.max(1, p.outputs().size()));
        int outRows = (p.outputs().size() + outCols - 1) / outCols;
        int width = inCols * 18 + 4 + 22 + 4 + outCols * 18;
        int height = Math.max(inRows, outRows) * 18;
        int bxr = x0 + (PAGE_W - width) / 2;
        int inY = y + (height - inRows * 18) / 2;
        int inSlots = p.grid() ? 9 : inputs.size();
        for (int i = 0; i < inSlots; i++) {
            int sx = bxr + (i % inCols) * 18;
            int sy = inY + (i / inCols) * 18;
            this.slot(g, sx, sy);
            ItemStack s = i < inputs.size() ? inputs.get(i) : null;
            if (s != null && slots != null) slots.add(new Slot(sx + 1, sy + 1, s));
        }
        int ax = bxr + inCols * 18 + 4;
        if (g != null) g.blit(RenderPipelines.GUI_TEXTURED, ARROW_TEXTURE, ax, y + (height - 15) / 2, 90.0F, 35.0F, 22, 15, 256, 256);
        int ox = ax + 22 + 4;
        int outY = y + (height - outRows * 18) / 2;
        for (int i = 0; i < p.outputs().size(); i++) {
            int sx = ox + (i % outCols) * 18;
            int sy = outY + (i / outCols) * 18;
            this.slot(g, sx, sy);
            if (slots != null) slots.add(new Slot(sx + 1, sy + 1, p.outputs().get(i)));
        }
        return y + height + 6;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
