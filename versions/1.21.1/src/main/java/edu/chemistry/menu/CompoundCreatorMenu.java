package edu.chemistry.menu;

import edu.chemistry.data.Chemistry;
import edu.chemistry.registry.EduBlocks;
import edu.chemistry.registry.EduMenus;
import edu.chemistry.registry.EduSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Combine elements in a 3x3 grid to make a compound. The grid is shapeless; the amounts must match the formula. */
public class CompoundCreatorMenu extends AbstractContainerMenu {
    // Bedrock layout (184 wide): grid on the compound background, connector, output on the right
    public static final int GRID_X = 53;
    public static final int GRID_Y = 25;
    public static final int RESULT_X = 138;
    public static final int RESULT_Y = 45;

    private final ContainerLevelAccess access;
    private final Player player;
    private final SimpleContainer grid = new SimpleContainer(9) {
        @Override
        public void setChanged() {
            super.setChanged();
            CompoundCreatorMenu.this.slotsChanged(this);
        }
    };
    private final SimpleContainer result = new SimpleContainer(1);

    public CompoundCreatorMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public CompoundCreatorMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(EduMenus.COMPOUND_CREATOR, id);
        this.access = access;
        this.player = inventory.player;
        this.addSlot(new Slot(this.result, 0, RESULT_X, RESULT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                CompoundCreatorMenu.this.consumeGrid();
                CompoundCreatorMenu.this.access.execute((level, pos) ->
                    level.playSound(null, pos, EduSounds.LAB_PRODUCT, SoundSource.BLOCKS, 1.0F, 0.6F));
                super.onTake(player, stack);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(this.grid, col + row * 3, GRID_X + col * 18, GRID_Y + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return Chemistry.atomicNumber(stack) > 0;
                    }
                });
            }
        }
        this.addStandardInventorySlots(inventory, 12, 87);
    }

    /** recipe book: put a compound's elements into the grid */
    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (edu.chemistry.recipe.StationPlacement.handle(player, buttonId, edu.chemistry.registry.EduRecipes.COMPOUND, this.grid)) {
            this.slotsChanged(this.grid);
            this.broadcastChanges();
            return true;
        }
        return false;
    }

    private List<ItemStack> gridItems() {
        List<ItemStack> list = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) list.add(this.grid.getItem(i));
        return list;
    }

    @Override
    public void slotsChanged(Container container) {
        if (container == this.grid && !this.player.level().isClientSide()) {
            this.result.setItem(0, Chemistry.compoundFor(this.gridItems()));
            this.broadcastChanges();
        }
    }

    private void consumeGrid() {
        for (int i = 0; i < 9; i++) {
            this.grid.removeItemNoUpdate(i);
        }
        this.grid.setChanged();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index == 0) {
            if (!this.moveItemStackTo(stack, 10, 46, true)) return ItemStack.EMPTY;
            if (!stack.isEmpty()) {
                player.getInventory().placeItemBackInInventory(stack.copy());
                stack.setCount(0);
            }
            slot.onTake(player, original);
            return ItemStack.EMPTY;
        } else if (index < 10) {
            if (!this.moveItemStackTo(stack, 10, 46, false)) return ItemStack.EMPTY;
        } else if (Chemistry.atomicNumber(stack) > 0) {
            if (!this.moveItemStackTo(stack, 1, 10, false)) return ItemStack.EMPTY;
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.result.setItem(0, ItemStack.EMPTY);
        this.access.execute((level, pos) -> this.clearContainer(player, this.grid));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, EduBlocks.COMPOUND_CREATOR);
    }

    /** the player's inventory (three rows) and hotbar, like vanilla menus lay them out */
    private void addStandardInventorySlots(net.minecraft.world.entity.player.Inventory inventory, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new net.minecraft.world.inventory.Slot(inventory, col + row * 9 + 9, x + col * 18, y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) this.addSlot(new net.minecraft.world.inventory.Slot(inventory, col, x + col * 18, y + 58));
    }
}
