package edu.chemistry.registry;

import edu.chemistry.EduMod;
import edu.chemistry.recipe.StationRecipe;
import java.lang.reflect.Constructor;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

/** Recipe types, serializers and recipe-book categories for the compound creator and lab table recipe books. */
public final class EduRecipes {
    public static StationRecipe.Station COMPOUND;
    public static StationRecipe.Station LAB;

    /** the recipe types (the stations themselves are made here, and filled in by the next two steps) */
    public static void initTypes() {
        COMPOUND = station("compound", () -> EduItems.COMPOUND_CREATOR);
        LAB = station("lab_table", () -> EduItems.LAB_TABLE);
    }

    public static void initSerializers() {
        for (StationRecipe.Station s : new StationRecipe.Station[] {COMPOUND, LAB}) {
            s.serializer = Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, EduMod.id(s.name),
                new RecipeSerializer<>(StationRecipe.codec(s), StationRecipe.streamCodec(s)));
        }
    }

    public static void initCategories() {
        for (StationRecipe.Station s : new StationRecipe.Station[] {COMPOUND, LAB}) {
            s.category = Registry.register(BuiltInRegistries.RECIPE_BOOK_CATEGORY, EduMod.id(s.name), new RecipeBookCategory());
        }
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

    private EduRecipes() {
    }
}
