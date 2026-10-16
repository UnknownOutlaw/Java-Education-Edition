package edu.chemistry.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
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
        public RecipeBookCategory category;

        public Station(String name, Supplier<Item> block) {
            this.name = name;
            this.block = block;
        }
    }

    private final Station station;
    private final List<ItemStackTemplate> ingredients;
    private final ItemStackTemplate result;

    public StationRecipe(Station station, List<ItemStackTemplate> ingredients, ItemStackTemplate result) {
        this.station = station;
        this.ingredients = List.copyOf(ingredients);
        this.result = result;
    }

    public static MapCodec<StationRecipe> codec(Station station) {
        return RecordCodecBuilder.mapCodec(i -> i.group(
            ItemStackTemplate.CODEC.listOf().fieldOf("ingredients").forGetter(r -> r.ingredients),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.result)
        ).apply(i, (ingredients, result) -> new StationRecipe(station, ingredients, result)));
    }

    public static StreamCodec<RegistryFriendlyByteBuf, StationRecipe> streamCodec(Station station) {
        return StreamCodec.composite(
            ItemStackTemplate.STREAM_CODEC.apply(ByteBufCodecs.list()), r -> r.ingredients,
            ItemStackTemplate.STREAM_CODEC, r -> r.result,
            (ingredients, result) -> new StationRecipe(station, ingredients, result));
    }

    public Station station() {
        return this.station;
    }

    public List<ItemStackTemplate> ingredients() {
        return this.ingredients;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return this.result.create();
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<? extends Recipe<CraftingInput>> getSerializer() {
        return this.station.serializer;
    }

    @Override
    public RecipeType<? extends Recipe<CraftingInput>> getType() {
        return this.station.type;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return this.station.category;
    }

    /** Each ingredient once, with its amount as the stack size; the table as the crafting station. */
    @Override
    public List<RecipeDisplay> display() {
        List<SlotDisplay> slots = new ArrayList<>();
        for (ItemStackTemplate t : this.ingredients) slots.add(new SlotDisplay.ItemStackSlotDisplay(t));
        return List.of(new ShapelessCraftingRecipeDisplay(slots, new SlotDisplay.ItemStackSlotDisplay(this.result),
            new SlotDisplay.ItemSlotDisplay(this.station.block.get())));
    }
}
