package edu.chemistry.recipe;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Server side of the recipe books: clicking a recipe moves its ingredients from your inventory into the table. */
public final class StationPlacement {
    /** menu button ids from here up mean "place this table's recipe #(id - PLACE_BASE)" (see StationRecipe.all) */
    public static final int PLACE_BASE = 1_000_000;

    /** @return true if the button id was a recipe placement (handled or not) */
    public static boolean handle(Player player, int buttonId, StationRecipe.Station station, Container grid) {
        if (buttonId < PLACE_BASE) return false;
        if (!(player.level() instanceof ServerLevel level)) return true;
        java.util.List<net.minecraft.world.item.crafting.RecipeHolder<StationRecipe>> all = StationRecipe.all(level.getRecipeManager(), station);
        int index = buttonId - PLACE_BASE;
        if (index >= all.size() || !(player instanceof net.minecraft.server.level.ServerPlayer sp) || !sp.getRecipeBook().contains(all.get(index))) return true;
        StationRecipe recipe = all.get(index).value();
        Inventory inv = player.getInventory();
        // give back what's in the table first
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack s = grid.removeItemNoUpdate(i);
            if (!s.isEmpty()) inv.placeItemBackInInventory(s);
        }
        int slot = 0;
        for (ItemStack want : recipe.ingredients()) {
            if (slot >= grid.getContainerSize()) break;
            ItemStack wanted = want.copy();
            int left = want.getCount();
            ItemStack placed = ItemStack.EMPTY;
            for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
                ItemStack have = inv.getItem(i);
                if (have.isEmpty() || !ItemStack.isSameItemSameComponents(have, wanted)) continue;
                int n = Math.min(left, have.getCount());
                ItemStack moved = have.split(n);
                if (placed.isEmpty()) placed = moved;
                else placed.grow(moved.getCount());
                left -= n;
            }
            if (!placed.isEmpty()) grid.setItem(slot++, placed);
        }
        grid.setChanged();
        inv.setChanged();
        return true;
    }

    private StationPlacement() {
    }
}
