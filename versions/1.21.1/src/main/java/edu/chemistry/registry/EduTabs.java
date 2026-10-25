package edu.chemistry.registry;

import edu.chemistry.EduMod;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One "Education Edition" creative tab. Inside it, side tabs (drawn by CreativeScreenMixin) pick a category.
 */
public final class EduTabs {
    public static CreativeModeTab CHEMISTRY;
    /**
     * Legacy4J's console creative menu is in use: it gets one tab per category instead (see LegacyCreative), so this
     * tab keeps its items in search only
     */
    public static volatile boolean consoleCreative;

    public record Category(String key, Supplier<Item> icon, Supplier<List<Item>> items) {
        public Component title() {
            return Component.translatable("itemGroup.edu.category." + this.key);
        }
    }

    public static final List<Category> CATEGORIES = new ArrayList<>();

    private static List<Item> list(Object... parts) {
        List<Item> out = new ArrayList<>();
        for (Object o : parts) {
            if (o instanceof Item i) out.add(i);
            else if (o instanceof java.util.Collection<?> c) for (Object x : c) out.add((Item) x);
            else if (o instanceof Item[] arr) for (Item x : arr) out.add(x);
        }
        return out;
    }

    /** Education Edition's creative order: by Bedrock data value (salt = 0 ... sodium hypochlorite = 37) */
    private static final List<String> COMPOUND_ORDER = List.of("salt", "sodium_oxide", "lye", "magnesium_nitrate", "iron_sulfide",
        "lithium_hydride", "sodium_hydride", "calcium_bromide", "magnesium_oxide", "sodium_acetate", "luminol", "aluminum_oxide",
        "boron_trioxide", "soap", "polyethylene", "garbage", "magnesium_salts", "sulfate", "barium_sulfate", "potassium_chloride",
        "mercuric_chloride", "cerium_chloride", "tungsten_chloride", "calcium_chloride", "water", "glue", "hypochlorite", "crude_oil",
        "latex", "potassium_iodide", "sodium_fluoride", "benzene", "hydrogen_peroxide", "ammonia", "sodium_hypochlorite");

    private static List<Item> compounds() {
        List<Item> out = new ArrayList<>();
        for (String id : COMPOUND_ORDER) out.add(EduItems.compound(id));
        // anything not in Bedrock's list goes at the end
        for (Item item : EduItems.COMPOUNDS.values()) if (!out.contains(item)) out.add(item);
        return out;
    }

    /** the Products tab, laid out like Education Edition's creative inventory */
    private static List<Item> products() {
        List<Item> out = list(EduItems.SUPER_FERTILIZER, EduItems.ICE_BOMB, EduItems.BLEACH,
            EduItems.UNDERWATER_TORCH, EduItems.UNDERWATER_TNT, EduItems.HEAT_BLOCK);
        for (String c : List.of("blue", "purple", "red", "green")) out.add(EduItems.COLORED_TORCHES.get(c));
        out.addAll(list(EduItems.EYE_DROPS, EduItems.TONIC, EduItems.ANTIDOTE, EduItems.ELIXIR));
        out.add(EduItems.HARD_GLASS);
        out.add(EduItems.HARD_GLASS_PANE);
        for (DyeColor c : DyeColor.values()) out.add(EduItems.HARD_STAINED_GLASS.get(c));
        for (DyeColor c : DyeColor.values()) out.add(EduItems.HARD_STAINED_GLASS_PANE.get(c));
        for (DyeColor c : DyeColor.values()) out.add(EduItems.BALLOONS.get(c));
        // glow sticks run backwards through the dye colours (red first, white last)
        List<DyeColor> sticks = new ArrayList<>(EduItems.GLOW_STICK_COLORS);
        java.util.Collections.reverse(sticks);
        for (DyeColor c : sticks) out.add(EduItems.GLOW_STICKS.get(c));
        for (String c : List.of("blue", "green", "orange", "purple", "red")) out.add(EduItems.SPARKLERS.get(c));
        return out;
    }

    /** every item in the mod, in category order */
    public static List<Item> allItems() {
        List<Item> out = new ArrayList<>();
        for (Category c : CATEGORIES) out.addAll(c.items().get());
        return out;
    }

    public static void init() {
        CATEGORIES.add(new Category("education", () -> EduItems.SLATE, () -> list(EduItems.BORDER, EduItems.ALLOW, EduItems.DENY,
            EduItems.SLATE, EduItems.POSTER, EduItems.BOARD, EduItems.CAMERA, EduItems.PORTFOLIO, EduItems.NPC_SPAWN_EGG,
            EduItems.AGENT_SPAWN_EGG)));
        CATEGORIES.add(new Category("lab", () -> EduItems.ELEMENT_CONSTRUCTOR, () -> list(EduItems.ELEMENT_CONSTRUCTOR,
            EduItems.COMPOUND_CREATOR, EduItems.LAB_TABLE, EduItems.MATERIAL_REDUCER, EduItems.GUIDE_BOOK)));
        CATEGORIES.add(new Category("elements", () -> EduItems.ELEMENTS[1], () -> list((Object) EduItems.ELEMENTS)));
        CATEGORIES.add(new Category("compounds", () -> EduItems.compound("water"), EduTabs::compounds));
        CATEGORIES.add(new Category("items", () -> EduItems.BALLOONS.get(DyeColor.RED), EduTabs::products));

        CreativeModeTab.Builder builder = fabricBuilder();
        CHEMISTRY = builder
            .title(Component.translatable("itemGroup.edu.chemistry"))
            .icon(() -> new ItemStack(EduItems.PORTFOLIO))
            .displayItems((parameters, output) -> {
                // like Bedrock, the tab (and the items in search) only show up in worlds with Education Edition turned on
                if (!edu.chemistry.EduRules.clientEnabled) return;
                CreativeModeTab.TabVisibility visibility = consoleCreative ? CreativeModeTab.TabVisibility.SEARCH_TAB_ONLY
                    : CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS;
                for (Item item : allItems()) output.accept(new ItemStack(item), visibility);
            })
            .build();
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, EduMod.id("chemistry"), CHEMISTRY);
    }

    /** makes the creative screen rebuild its tabs (and search) next time it opens (the cache is opened by the access widener) */
    public static void refresh() {
        net.minecraft.world.item.CreativeModeTabs.CACHED_PARAMETERS = null;
    }

    /** Fabric API's builder gives the tab its own page; fall back to a plain vanilla builder if it's missing. */
    private static CreativeModeTab.Builder fabricBuilder() {
        // a fixed spot in the vanilla tab rows (where it has always been), rather than a Fabric API tab page
        return CreativeModeTab.builder(CreativeModeTab.Row.BOTTOM, 6);
    }

    private EduTabs() {
    }
}
