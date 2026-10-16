package edu.chemistry.photo;

import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Photo items carry their picture with them (a small PNG in the item's custom data), so a photo can be given to other
 * players, put in chests and added to anyone's portfolio.
 */
public final class Photos {
    public static final String KEY_IMAGE = "edu_photo";
    public static final String KEY_CAPTION = "edu_caption";
    public static final String KEY_ID = "edu_photo_id";
    /** largest picture a photo item may carry */
    public static final int MAX_BYTES = 256 * 1024;

    public static void write(ItemStack stack, String id, byte[] png, String caption) {
        CompoundTag tag = new CompoundTag();
        tag.putString(KEY_ID, id);
        tag.putByteArray(KEY_IMAGE, png);
        tag.putString(KEY_CAPTION, caption);
        CustomData.set(DataComponents.CUSTOM_DATA, stack, tag);
    }

    private static CompoundTag tag(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }

    public static Optional<byte[]> image(ItemStack stack) {
        return tag(stack).getByteArray(KEY_IMAGE);
    }

    public static String caption(ItemStack stack) {
        return tag(stack).getStringOr(KEY_CAPTION, "");
    }

    public static String id(ItemStack stack) {
        return tag(stack).getStringOr(KEY_ID, "");
    }

    private Photos() {
    }
}
