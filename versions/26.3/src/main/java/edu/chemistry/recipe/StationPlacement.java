package edu.chemistry.recipe;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;

/** Server side of the recipe books: clicking a recipe moves its ingredients from your inventory into the table. */
public final class StationPlacement {
    /** menu button ids from here up mean "place recipe display #(id - PLACE_BASE)" */
    public static final int PLACE_BASE = 1_000_000;

    /** @return true if the button id was a recipe placement (handled or not) */
    public static boolean handle(Player player, int buttonId, StationRecipe.Station station, Container grid) {
        if (buttonId < PLACE_BASE) return false;
        if (!(player.level() instanceof ServerLevel level)) return true;
        RecipeManager.ServerDisplayInfo info = level.recipeAccess().getRecipeFromDisplay(new RecipeDisplayId(buttonId - PLACE_BASE));
        if (info == null || !(info.parent().value() instanceof StationRecipe recipe) || recipe.station() != station) return true;
        Inventory inv = player.getInventory();
        // give back what's in the table first
        for (int i = 0; i < grid.getContainerSize(); i++) {
            ItemStack s = grid.removeItemNoUpdate(i);
            if (!s.isEmpty()) inv.placeItemBackInInventory(s, net.minecraft.util.Prediction.SERVER_ONLY);
        }
        int slot = 0;
        for (ItemStackTemplate want : recipe.ingredients()) {
            if (slot >= grid.getContainerSize()) break;
            ItemStack wanted = want.create();
            int left = want.count();
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
