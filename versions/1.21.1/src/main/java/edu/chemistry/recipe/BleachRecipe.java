package edu.chemistry.recipe;

import edu.chemistry.registry.EduItems;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Bleach turns dyed leather white, keeping everything else about the item (enchantments, wear, name). The newer
 * builds do this with "transmute" recipes, which 1.21.1 doesn't have.
 */
public class BleachRecipe extends CustomRecipe {
    /** white dye's colour */
    public static final int WHITE = 0xF9FFFE;
    private static final Set<Item> LEATHER = Set.of(Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS,
        Items.LEATHER_BOOTS, Items.LEATHER_HORSE_ARMOR);
    public static RecipeSerializer<BleachRecipe> SERIALIZER;

    public BleachRecipe(CraftingBookCategory category) {
        super(category);
    }

    /** the leather item, if the grid holds exactly one leather item and one bleach */
    private static ItemStack leather(CraftingInput input) {
        ItemStack leather = ItemStack.EMPTY;
        int bleach = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty()) continue;
            if (s.is(EduItems.BLEACH)) bleach++;
            else if (LEATHER.contains(s.getItem()) && leather.isEmpty()) leather = s;
            else return ItemStack.EMPTY;
        }
        return bleach == 1 ? leather : ItemStack.EMPTY;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return !leather(input).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack out = leather(input).copyWithCount(1);
        if (out.isEmpty()) return ItemStack.EMPTY;
        DyedItemColor old = out.get(DataComponents.DYED_COLOR);
        out.set(DataComponents.DYED_COLOR, new DyedItemColor(WHITE, old == null || old.showInTooltip()));
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }
}
