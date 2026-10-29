package edu.chemistry.registry;

import edu.chemistry.EduMod;
import edu.chemistry.recipe.StationRecipe;
import java.lang.reflect.Constructor;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/** Recipe types, serializers and recipe-book categories for the compound creator and lab table recipe books. */
public final class EduRecipes {
    public static StationRecipe.Station COMPOUND;
    public static StationRecipe.Station LAB;

    /** the recipe types (the stations themselves are made here, and get their serializers in the next step) */
    public static void initTypes() {
        COMPOUND = station("compound", () -> EduItems.COMPOUND_CREATOR);
        LAB = station("lab_table", () -> EduItems.LAB_TABLE);
    }

    public static void initSerializers() {
        for (StationRecipe.Station s : new StationRecipe.Station[] {COMPOUND, LAB}) s.serializer = serializer(s);
        edu.chemistry.recipe.BleachRecipe.SERIALIZER = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, EduMod.id("bleach_leather"),
            new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(edu.chemistry.recipe.BleachRecipe::new));
    }

    private static StationRecipe.Station station(String name, java.util.function.Supplier<net.minecraft.world.item.Item> block) {
        try {
            Constructor<StationRecipe.Station> c = StationRecipe.Station.class.getDeclaredConstructor(String.class, java.util.function.Supplier.class);
            c.setAccessible(true);
            StationRecipe.Station s = c.newInstance(name, block);
            String id = EduMod.id(name).toString();
            s.type = Registry.register(BuiltInRegistries.RECIPE_TYPE, EduMod.id(name), new RecipeType<StationRecipe>() {
                @Override
                public String toString() {
                    return id;
                }
            });
            return s;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static RecipeSerializer<StationRecipe> serializer(StationRecipe.Station s) {
        com.mojang.serialization.MapCodec<StationRecipe> codec = StationRecipe.codec(s);
        net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, StationRecipe> stream = StationRecipe.streamCodec(s);
        return Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, EduMod.id(s.name), new RecipeSerializer<StationRecipe>() {
            @Override
            public com.mojang.serialization.MapCodec<StationRecipe> codec() {
                return codec;
            }

            @Override
            public net.minecraft.network.codec.StreamCodec<net.minecraft.network.RegistryFriendlyByteBuf, StationRecipe> streamCodec() {
                return stream;
            }
        });
    }

    private EduRecipes() {
    }
}
