package edu.chemistry.menu;

import edu.chemistry.data.Chemistry;
import edu.chemistry.data.ChemData;
import edu.chemistry.registry.EduBlocks;
import edu.chemistry.registry.EduItems;
import edu.chemistry.registry.EduMenus;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Pick a number of protons, neutrons and electrons to build an element (or one of its isotopes).
 * Put an element in the output slot to see what it is made of.
 */
public class ElementConstructorMenu extends AbstractContainerMenu {
    public static final int PROTONS = 0;
    public static final int NEUTRONS = 1;
    public static final int ELECTRONS = 2;
    // Bedrock layout (250x220): sliders on the left, Bohr model + microscope + output on the right, inventory bottom right
    public static final int OUTPUT_X = 212;
    public static final int OUTPUT_Y = 90;
    public static final int INV_X = 84;
    public static final int INV_Y = 139;

    private final ContainerLevelAccess access;
    private final Player player;
    private final DataSlot protons = DataSlot.standalone();
    private final DataSlot neutrons = DataSlot.standalone();
    private final DataSlot electrons = DataSlot.standalone();
    private final SimpleContainer output = new SimpleContainer(1);
    private final Slot outputSlot;
    private boolean updating;

    public ElementConstructorMenu(int id, Inventory inventory) {
        this(id, inventory, ContainerLevelAccess.NULL);
    }

    public ElementConstructorMenu(int id, Inventory inventory, ContainerLevelAccess access) {
        super(EduMenus.ELEMENT_CONSTRUCTOR, id);
        this.access = access;
        this.player = inventory.player;
        this.outputSlot = this.addSlot(new Slot(this.output, 0, OUTPUT_X, OUTPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return Chemistry.atomicNumber(stack) > 0;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                ElementConstructorMenu.this.onOutputChanged();
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
                player.level().playSound(null, player.blockPosition(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.3F, 1.6F);
                ElementConstructorMenu.this.refreshOutput();
            }
        });
        this.addStandardInventorySlots(inventory, INV_X, INV_Y);
        this.addDataSlot(this.protons);
        this.addDataSlot(this.neutrons);
        this.addDataSlot(this.electrons);
    }

    public int get(int which) {
        return switch (which) {
            case PROTONS -> this.protons.get();
            case NEUTRONS -> this.neutrons.get();
            default -> this.electrons.get();
        };
    }

    public static int max(int which) {
        return switch (which) {
            case PROTONS -> Chemistry.MAX_PROTONS;
            case NEUTRONS -> Chemistry.MAX_NEUTRONS;
            default -> Chemistry.MAX_ELECTRONS;
        };
    }

    /** Button ids: which * 1000 + value */
    public static int buttonId(int which, int value) {
        return which * 1000 + value;
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        int which = buttonId / 1000;
        int value = buttonId % 1000;
        if (which < 0 || which > 2 || value < 0) return false;
        value = Math.min(value, max(which));
        switch (which) {
            case PROTONS -> this.protons.set(value);
            case NEUTRONS -> this.neutrons.set(value);
            default -> this.electrons.set(value);
        }
        this.refreshOutput();
        return true;
    }

    /** -1 if the current particles don't make an element */
    public int currentElement() {
        return Chemistry.construct(this.protons.get(), this.neutrons.get(), this.electrons.get());
    }

    public boolean isIsotope() {
        int z = this.currentElement();
        return z > 0 && this.neutrons.get() != Chemistry.mainNeutrons(z);
    }

    private void refreshOutput() {
        if (this.player.level().isClientSide()) return;
        this.updating = true;
        int z = this.currentElement();
        this.output.setItem(0, z > 0 ? new ItemStack(EduItems.ELEMENTS[z]) : ItemStack.EMPTY);
        this.updating = false;
        this.broadcastChanges();
    }

    /** A player dropped an element into the output: show its particles and hand the element back. */
    private void onOutputChanged() {
        if (this.updating || this.player.level().isClientSide()) return;
        ItemStack placed = this.output.getItem(0);
        int z = Chemistry.atomicNumber(placed);
        if (z <= 0) return;
        if (placed.getCount() == 1 && this.currentElement() == z) return;
        this.protons.set(z);
        this.electrons.set(z);
        this.neutrons.set(Chemistry.mainNeutrons(z));
        this.updating = true;
        this.output.setItem(0, ItemStack.EMPTY);
        this.updating = false;
        this.player.getInventory().placeItemBackInInventory(placed);
        this.refreshOutput();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        if (index == 0) {
            // shift-click builds a full stack
            ItemStack full = stack.copyWithCount(stack.getMaxStackSize());
            if (!this.moveItemStackTo(full, 1, 37, true)) return ItemStack.EMPTY;
            this.refreshOutput();
            return ItemStack.EMPTY;
        }
        if (Chemistry.atomicNumber(stack) > 0 && !this.outputSlot.hasItem()) {
            ItemStack one = stack.split(1);
            this.outputSlot.set(one);
            slot.setChanged();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // the constructed element is free and just disappears
        this.output.setItem(0, ItemStack.EMPTY);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, EduBlocks.ELEMENT_CONSTRUCTOR);
    }

    public static String symbol(int z) {
        return z >= 0 && z < ChemData.SYMBOLS.length ? ChemData.SYMBOLS[z] : "?";
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
