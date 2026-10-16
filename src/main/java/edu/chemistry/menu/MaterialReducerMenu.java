package edu.chemistry.menu;

import edu.chemistry.data.Chemistry;
import edu.chemistry.registry.EduBlocks;
import edu.chemistry.registry.EduMenus;
import edu.chemistry.registry.EduSounds;
import java.util.List;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Breaks one item into the elements it is made of. The outputs are a preview until you take one; then one input item is
 * used up and the input is locked until every output has been taken.
 */
public class MaterialReducerMenu extends AbstractContainerMenu {
    // Bedrock layout: input on top, then 5 outputs and a staggered row of 4 below
    public static final int INPUT_X = 95;
    public static final int INPUT_Y = 19;
    public static final int INV_X = 23;
    public static final int INV_Y = 111;

    public static int outputX(int i) {
        return i < 5 ? 43 + i * 26 : 56 + (i - 5) * 26;
    }

    public static int outputY(int i) {
        return i < 5 ? 54 : 79;
    }

    private final ContainerLevelAccess access;
    private final Player player;
    private final SimpleContainer input = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            MaterialReducerMenu.this.slotsChanged(this);
        }
    };
    private final SimpleContainer outputs = new SimpleContainer(9);
    /** 1 while the outputs are real items (input used up and locked) */
    private final DataSlot committed = DataSlot.standalone();
    /** 1 if the input item can't be reduced */
    private final DataSlot unknown = DataSlot.standalone();
    private boolean updating;

    public MaterialReducerMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public MaterialReducerMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(EduMenus.MATERIAL_REDUCER, id);
        this.access = access;
        this.player = inventory.player;
        this.addSlot(new Slot(this.input, 0, INPUT_X, INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return MaterialReducerMenu.this.committed.get() == 0;
            }
        });
        for (int i = 0; i < 9; i++) {
            {
                this.addSlot(new Slot(this.outputs, i, outputX(i), outputY(i)) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public void onTake(Player player, ItemStack stack) {
                        MaterialReducerMenu.this.onOutputTaken();
                        super.onTake(player, stack);
                    }
                });
            }
        }
        this.addStandardInventorySlots(inventory, INV_X, INV_Y);
        this.addDataSlot(this.committed);
        this.addDataSlot(this.unknown);
    }

    public boolean isLocked() {
        return this.committed.get() != 0;
    }

    public boolean isUnknown() {
        return this.unknown.get() != 0;
    }

    @Override
    public void slotsChanged(Container container) {
        if (container == this.input && !this.updating && !this.player.level().isClientSide() && this.committed.get() == 0) {
            this.preview();
        }
    }

    private void preview() {
        this.updating = true;
        this.outputs.clearContent();
        ItemStack in = this.input.getItem(0);
        List<ItemStack> result = Chemistry.reduce(in);
        for (int i = 0; i < result.size() && i < 9; i++) {
            this.outputs.setItem(i, result.get(i));
        }
        this.unknown.set(!in.isEmpty() && result.isEmpty() ? 1 : 0);
        this.updating = false;
        this.broadcastChanges();
    }

    private void onOutputTaken() {
        if (this.player.level().isClientSide()) return;
        if (this.committed.get() == 0) {
            // first output taken: the input item is used up
            this.updating = true;
            this.input.removeItem(0, 1);
            this.updating = false;
            this.committed.set(1);
            this.access.execute((level, pos) -> level.playSound(null, pos, EduSounds.LAB_FIZZ, SoundSource.BLOCKS, 0.6F, 1.2F));
        }
        boolean empty = true;
        for (int i = 0; i < 9; i++) {
            if (!this.outputs.getItem(i).isEmpty()) empty = false;
        }
        if (empty) {
            this.committed.set(0);
            this.preview();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index >= 1 && index <= 9) {
            if (!this.moveItemStackTo(stack, 10, 46, true)) return ItemStack.EMPTY;
            if (!stack.isEmpty()) {
                player.getInventory().placeItemBackInInventory(stack.copy());
                stack.setCount(0);
            }
            slot.setChanged();
            slot.onTake(player, original);
            return ItemStack.EMPTY;
        } else if (index == 0) {
            if (!this.moveItemStackTo(stack, 10, 46, false)) return ItemStack.EMPTY;
        } else {
            if (this.committed.get() != 0 || !this.moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (this.committed.get() == 0) {
            this.outputs.clearContent();
        }
        this.access.execute((level, pos) -> {
            this.clearContainer(player, this.input);
            this.clearContainer(player, this.outputs);
        });
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, EduBlocks.MATERIAL_REDUCER);
    }
}
