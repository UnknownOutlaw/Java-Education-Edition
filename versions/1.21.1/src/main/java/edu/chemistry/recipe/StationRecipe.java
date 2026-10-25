package edu.chemistry.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * A compound creator or lab table recipe, only used for the recipe books of those two tables (and for unlocking them):
 * the tables themselves still work out what you made from what's in them. Ingredients carry their amounts.
 */
public class StationRecipe implements Recipe<CraftingInput> {
    /** One of the two chemistry tables with a recipe book. */
    public static final class Station {
        public final String name;
        public final Supplier<Item> block;
        public RecipeType<StationRecipe> type;
        public RecipeSerializer<StationRecipe> serializer;

        public Station(String name, Supplier<Item> block) {
            this.name = name;
            this.block = block;
        }
    }

    private final Station station;
    private final List<ItemStack> ingredients;
    private final ItemStack result;

    public StationRecipe(Station station, List<ItemStack> ingredients, ItemStack result) {
        this.station = station;
        this.ingredients = List.copyOf(ingredients);
        this.result = result;
    }

    public static MapCodec<StationRecipe> codec(Station station) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ItemStack.CODEC.listOf().fieldOf("ingredients").forGetter(r -> r.ingredients),
            ItemStack.CODEC.fieldOf("result").forGetter(r -> r.result)
        ).apply(i, (ingredients, result) -> new StationRecipe(station, ingredients, result)));
    }

    public static StreamCodec<RegistryFriendlyByteBuf, StationRecipe> streamCodec(Station station) {
        return StreamCodec.composite(
            ItemStack.LIST_STREAM_CODEC, r -> r.ingredients,
            ItemStack.STREAM_CODEC, r -> r.result,
            (ingredients, result) -> new StationRecipe(station, ingredients, result));
    }

    /**
     * This table's recipes in a fixed order (by recipe id), the same on the server and the client: the recipe book asks
     * for a recipe by its place in this list.
     */
    public static List<RecipeHolder<StationRecipe>> all(RecipeManager manager, Station station) {
        List<RecipeHolder<StationRecipe>> list = new ArrayList<>(manager.getAllRecipesFor(station.type));
        list.sort(Comparator.comparing(h -> h.id().toString()));
        return list;
    }

    public Station station() {
        return this.station;
    }

    /** each ingredient once, with its amount as the stack size */
    public List<ItemStack> ingredients() {
        return this.ingredients;
    }

    public ItemStack result() {
        return this.result;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        NonNullList<Ingredient> list = NonNullList.create();
        for (ItemStack s : this.ingredients) list.add(Ingredient.of(s.getItem()));
        return list;
    }

    /** kept out of the vanilla crafting book (the tables have their own) */
    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    @Override
    public String getGroup() {
        return "";
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(this.station.block.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return this.station.serializer;
    }

    @Override
    public RecipeType<?> getType() {
        return this.station.type;
    }
}
