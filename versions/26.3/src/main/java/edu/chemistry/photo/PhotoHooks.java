package edu.chemistry.photo;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

/** Client actions the (common) camera, portfolio and photo items trigger; the client fills these in. */
public final class PhotoHooks {
    /** take a picture: (closeUp, the entity to take it from: the player, or a tripod camera) */
    public static BiConsumer<Boolean, Entity> takePicture = (c, e) -> {
    };
    public static Runnable openPortfolio = () -> {
    };
    public static Consumer<ItemStack> viewPhoto = s -> {
    };

    private PhotoHooks() {
    }
}
