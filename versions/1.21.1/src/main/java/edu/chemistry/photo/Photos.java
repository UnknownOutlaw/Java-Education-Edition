package edu.chemistry.photo;

import java.util.Optional;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Photo items carry their picture with them (a JPEG, or a PNG for photos saved before 1.0.122, in the item's custom
 * data), so a photo can be given to other players, put in chests and added to anyone's portfolio.
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
        CompoundTag tag = tag(stack);
        return tag.contains(KEY_IMAGE, net.minecraft.nbt.Tag.TAG_BYTE_ARRAY) ? Optional.of(tag.getByteArray(KEY_IMAGE)) : Optional.empty();
    }

    public static String caption(ItemStack stack) {
        return tag(stack).getString(KEY_CAPTION);
    }

    /** whether these bytes look like a PNG */
    public static boolean isPng(byte[] b) {
        return b.length > 8 && b[0] == (byte) 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
    }

    /** whether these bytes look like a JPEG */
    public static boolean isJpeg(byte[] b) {
        return b.length > 4 && b[0] == (byte) 0xFF && b[1] == (byte) 0xD8 && b[2] == (byte) 0xFF;
    }

    public static String id(ItemStack stack) {
        return tag(stack).getString(KEY_ID);
    }

    private Photos() {
    }
}
