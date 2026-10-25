package edu.chemistry.compat.jei;

import edu.chemistry.EduMod;
import edu.chemistry.client.CreativeCategories;
import edu.chemistry.client.EduContainerScreen;
import edu.chemistry.data.ChemData;
import edu.chemistry.data.Chemistry;
import edu.chemistry.data.LabReactions;
import edu.chemistry.registry.EduItems;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.handlers.IGlobalGuiHandler;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.AbstractRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

/**
 * JEI support (only loaded when JEI is installed, through the "jei_mod_plugin" entrypoint): the Compound Creator, Lab
 * Table, Material Reducer and Element Constructor get their own recipe pages, the tables show up as their crafting
 * stations, items without a recipe get an info page, and JEI's item list keeps clear of the Education Edition
 * creative side tabs and the tables' recipe books.
 */
public class EduJeiPlugin implements IModPlugin {
    /** one recipe on a page: what goes in (with how many), what comes out, and an optional line of text */
    public record Reaction(List<ItemStack> inputs, List<ItemStack> outputs, @Nullable Component note) {
    }

    public static final IRecipeType<Reaction> COMPOUND = IRecipeType.create(EduMod.MOD_ID, "compound_creator", Reaction.class);
    public static final IRecipeType<Reaction> LAB = IRecipeType.create(EduMod.MOD_ID, "lab_table", Reaction.class);
    public static final IRecipeType<Reaction> REDUCER = IRecipeType.create(EduMod.MOD_ID, "material_reducer", Reaction.class);
    public static final IRecipeType<Reaction> CONSTRUCTOR = IRecipeType.create(EduMod.MOD_ID, "element_constructor", Reaction.class);

    @Override
    public Identifier getPluginUid() {
        return EduMod.id("jei");
    }

    // ---------------------------------------------------------------- the item list
    // The Education Edition creative tab is empty unless the world has Education Edition turned on, so JEI (which
    // builds its item list from the creative tabs when you join) can miss every item in the mod. Once JEI is running,
    // the items are added to (or taken out of) its list to match the world's Education Edition setting.
    private static mezz.jei.api.runtime.@Nullable IJeiRuntime runtime;
    private static boolean listed;

    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        listed = false;
        for (ItemStack s : jeiRuntime.getIngredientManager().getAllIngredients(mezz.jei.api.constants.VanillaTypes.ITEM_STACK)) {
            if (s.is(EduItems.LAB_TABLE)) {
                listed = true;
                break;
            }
        }
        edu.chemistry.EduRules.onClientEnabledChanged = EduJeiPlugin::syncItemList;
        syncItemList();
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
        edu.chemistry.EduRules.onClientEnabledChanged = () -> {
        };
    }

    private static void syncItemList() {
        mezz.jei.api.runtime.IJeiRuntime r = runtime;
        if (r == null) return;
        boolean want = edu.chemistry.EduRules.clientEnabled;
        if (want == listed) return;
        List<ItemStack> stacks = new ArrayList<>();
        for (Item item : edu.chemistry.registry.EduTabs.allItems()) stacks.add(new ItemStack(item));
        try {
            if (want) r.getIngredientManager().addIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, stacks);
            else r.getIngredientManager().removeIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, stacks);
            listed = want;
            EduMod.LOG.info("JEI: {} the {} Education Edition items", want ? "showing" : "hiding", stacks.size());
        } catch (RuntimeException e) {
            EduMod.LOG.warn("Couldn't update JEI's item list", e);
        }
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
            new Category(COMPOUND, gui, EduItems.COMPOUND_CREATOR, 4, 1, true),
            new Category(LAB, gui, EduItems.LAB_TABLE, 4, 1, false),
            new Category(REDUCER, gui, EduItems.MATERIAL_REDUCER, 1, 3, false),
            new Category(CONSTRUCTOR, gui, EduItems.ELEMENT_CONSTRUCTOR, 0, 1, true));
    }

    private static ItemStack element(int z, int count) {
        return new ItemStack(EduItems.ELEMENTS[z], count);
    }

    private static List<ItemStack> elements(int[] pairs) {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) out.add(element(pairs[i], pairs[i + 1]));
        return out;
    }

    private static @Nullable Component formula(int i) {
        String f = i < ChemData.COMPOUND_FORMULAS.length ? ChemData.COMPOUND_FORMULAS[i] : "";
        return f.isEmpty() ? null : Component.literal(f);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // compound creator: every compound with a recipe, then the natural compounds (charcoal, ink sac, sugar)
        List<Reaction> compounds = new ArrayList<>();
        for (int i = 0; i < ChemData.COMPOUND_IDS.length; i++) {
            int[] recipe = ChemData.COMPOUND_RECIPES[i];
            if (recipe.length == 0) continue;
            compounds.add(new Reaction(elements(recipe), List.of(new ItemStack(EduItems.compound(ChemData.COMPOUND_IDS[i]))), formula(i)));
        }
        for (int i = 0; i < ChemData.NATURAL_ITEMS.length; i++) {
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(ChemData.NATURAL_ITEMS[i]));
            compounds.add(new Reaction(elements(ChemData.NATURAL_RECIPES[i]), List.of(new ItemStack(item)), null));
        }
        registration.addRecipes(COMPOUND, compounds);

        // lab table reactions
        List<Reaction> lab = new ArrayList<>();
        for (LabReactions.Shown r : LabReactions.shown()) lab.add(new Reaction(r.inputs(), List.of(r.result()), null));
        registration.addRecipes(LAB, lab);

        // material reducer: every block or item it can break down (sorted by id), and every compound
        List<Reaction> reduce = new ArrayList<>();
        Map<String, int[]> sorted = new TreeMap<>(ChemData.REDUCTIONS);
        for (String id : sorted.keySet()) {
            Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(id)).orElse(null);
            if (item == null) continue;
            ItemStack in = new ItemStack(item);
            List<ItemStack> out = Chemistry.reduce(in);
            if (!out.isEmpty()) reduce.add(new Reaction(List.of(in), out, null));
        }
        for (String id : ChemData.COMPOUND_IDS) {
            ItemStack in = new ItemStack(EduItems.compound(id));
            List<ItemStack> out = Chemistry.reduce(in);
            if (!out.isEmpty()) reduce.add(new Reaction(List.of(in), out, null));
        }
        registration.addRecipes(REDUCER, reduce);

        // element constructor: how many protons, neutrons and electrons make each element
        List<Reaction> build = new ArrayList<>();
        for (int z = 1; z < EduItems.ELEMENTS.length; z++) {
            if (EduItems.ELEMENTS[z] == null) continue;
            int neutrons = Math.max(0, ChemData.MASS[z] - z);
            build.add(new Reaction(List.of(), List.of(element(z, 1)),
                Component.translatable("jei.edu.element_constructor.particles", z, neutrons, z)));
        }
        registration.addRecipes(CONSTRUCTOR, build);

        // medicines are brewing stand recipes in 26.3 (data/edu/recipe/brewing), which JEI shows on its own brewing page
        EduMod.LOG.info("JEI: added {} Compound Creator, {} Lab Table, {} Material Reducer and {} Element Constructor recipes",
            compounds.size(), lab.size(), reduce.size(), build.size());

        // things that aren't made from a recipe
        info(registration, EduItems.MATERIAL_REDUCER, "material_reducer");
        info(registration, EduItems.SLATE, "chalkboard");
        info(registration, EduItems.POSTER, "chalkboard");
        info(registration, EduItems.BOARD, "chalkboard");
        info(registration, EduItems.ALLOW, "allow");
        info(registration, EduItems.DENY, "deny");
        info(registration, EduItems.BORDER, "border");
        info(registration, EduItems.NPC_SPAWN_EGG, "npc");
        info(registration, EduItems.AGENT_SPAWN_EGG, "agent");
        info(registration, EduItems.CAMERA, "camera");
        info(registration, EduItems.PORTFOLIO, "portfolio");
    }

    private static void info(IRecipeRegistration registration, ItemLike item, String key) {
        registration.addIngredientInfo(item, Component.translatable("jei.edu.info." + key));
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addCraftingStation(COMPOUND, EduItems.COMPOUND_CREATOR);
        registration.addCraftingStation(LAB, EduItems.LAB_TABLE);
        registration.addCraftingStation(REDUCER, EduItems.MATERIAL_REDUCER);
        registration.addCraftingStation(CONSTRUCTOR, EduItems.ELEMENT_CONSTRUCTOR);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // keep JEI's panels off the tables' recipe books
        registration.addGenericGuiContainerHandler(EduContainerScreen.class, new IGuiContainerHandler<EduContainerScreen<?>>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(EduContainerScreen<?> screen) {
                List<Rect2i> out = new ArrayList<>();
                for (int[] r : screen.extraAreas()) out.add(new Rect2i(r[0], r[1], r[2], r[3]));
                return out;
            }
        });
        // and off the Education Edition tab's side tabs in the creative inventory
        registration.addGlobalGuiHandler(new IGlobalGuiHandler() {
            @Override
            public Collection<Rect2i> getGuiExtraAreas() {
                int[] r = CreativeCategories.shownArea;
                if (r == null || !(Minecraft.getInstance().gui.screen() instanceof CreativeModeInventoryScreen)) return List.of();
                return List.of(new Rect2i(r[0], r[1], r[2], r[3]));
            }
        });
    }

    /** A page of recipes: the ingredients in a row, an arrow, then what comes out (in a grid if there's a lot). */
    private static final class Category extends AbstractRecipeCategory<Reaction> {
        private static final int TEXT_H = 11;
        private final int inCols;
        private final int outRows;
        private final boolean note;

        Category(IRecipeType<Reaction> type, IGuiHelper gui, ItemLike station, int inCols, int outRows, boolean note) {
            super(type, new ItemStack(station).getHoverName(), gui.createDrawableItemLike(station),
                Math.max(width(inCols), note ? 170 : 0), outRows * 18 + (note ? TEXT_H : 0));
            this.inCols = inCols;
            this.outRows = outRows;
            this.note = note;
        }

        private static int width(int inCols) {
            return Math.max(inCols * 18, 18) + 4 + 24 + 4 + 3 * 18;
        }

        /** the recipe is centred on the page (the page is wider when it has a line of text under it) */
        private int offset() {
            return (this.getWidth() - width(this.inCols)) / 2;
        }

        private int arrowX() {
            return this.offset() + Math.max(this.inCols * 18, 18) + 4;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, Reaction recipe, IFocusGroup focuses) {
            int rowsH = this.outRows * 18;
            int n = recipe.inputs().size();
            int inY = (rowsH - 18) / 2;
            int inX = this.offset() + Math.max(this.inCols * 18, 18) - n * 18;
            for (int i = 0; i < n; i++) {
                builder.addInputSlot(inX + i * 18 + 1, inY + 1).setStandardSlotBackground().add(recipe.inputs().get(i));
            }
            int outs = recipe.outputs().size();
            int cols = Math.min(3, Math.max(1, outs));
            int rows = (outs + cols - 1) / cols;
            int ox = this.arrowX() + 24 + 4;
            int oy = (rowsH - rows * 18) / 2;
            for (int i = 0; i < outs; i++) {
                builder.addOutputSlot(ox + (i % cols) * 18 + 1, oy + (i / cols) * 18 + 1).setStandardSlotBackground()
                    .add(recipe.outputs().get(i));
            }
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, Reaction recipe, IFocusGroup focuses) {
            builder.addRecipeArrow().setPosition(this.arrowX(), (this.outRows * 18 - 17) / 2);
            if (this.note && recipe.note() != null) {
                builder.addText(recipe.note(), this.getWidth(), TEXT_H).setPosition(0, this.outRows * 18 + 2).setColor(0xFF808080)
                    .setTextAlignment(mezz.jei.api.gui.placement.HorizontalAlignment.CENTER);
            }
        }
    }
}
