package edu.chemistry.menu;

import edu.chemistry.data.LabReactions;
import edu.chemistry.registry.EduBlocks;
import edu.chemistry.registry.EduMenus;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Put up to nine ingredients on the table and press Combine. Products pop out of the top of the table. */
public class LabTableMenu extends AbstractContainerMenu {
    // Bedrock layout: a single row of nine ingredient slots under the beaker, flask and jar
    public static final int GRID_X = 8;
    public static final int GRID_Y = 68;
    public static final int BUTTON_COMBINE = 0;

    private final ContainerLevelAccess access;
    private final SimpleContainer grid = new SimpleContainer(9);

    public LabTableMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public LabTableMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(EduMenus.LAB_TABLE, id);
        this.access = access;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(this.grid, col, GRID_X + col * 18, GRID_Y));
        }
        this.addStandardInventorySlots(inventory, 8, 118);
    }

    public boolean hasIngredients() {
        for (int i = 0; i < 9; i++) {
            if (!this.grid.getItem(i).isEmpty()) return true;
        }
        return false;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (edu.chemistry.recipe.StationPlacement.handle(player, buttonId, edu.chemistry.registry.EduRecipes.LAB, this.grid)) {
            this.broadcastChanges();
            return true;
        }
        if (buttonId != BUTTON_COMBINE || !this.hasIngredients()) return false;
        this.access.execute((level, pos) -> {
            if (level instanceof ServerLevel server) {
                List<ItemStack> items = new ArrayList<>(9);
                for (int i = 0; i < 9; i++) items.add(this.grid.getItem(i));
                LabReactions.react(server, pos, player, items);
                this.grid.setChanged();
                this.broadcastChanges();
            }
        });
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < 9) {
            if (!this.moveItemStackTo(stack, 9, 45, true)) return ItemStack.EMPTY;
        } else if (!this.moveItemStackTo(stack, 0, 9, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.access.execute((level, pos) -> this.clearContainer(player, this.grid));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, EduBlocks.LAB_TABLE);
    }
}
