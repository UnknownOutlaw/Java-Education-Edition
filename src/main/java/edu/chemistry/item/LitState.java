package edu.chemistry.item;

import java.util.List;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

/** Sparklers and glow sticks store "lit" in custom model data flag 0, which also switches their item model. */
public final class LitState {
    public static boolean isLit(ItemStack stack) {
        CustomModelData data = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        if (data == null) return false;
        Boolean b = data.getBoolean(0);
        return b != null && b;
    }

    public static void setLit(ItemStack stack, boolean lit) {
        if (lit) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(true), List.of(), List.of()));
        } else {
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        }
    }

    private LitState() {
    }
}
