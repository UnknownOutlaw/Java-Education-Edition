package edu.chemistry.registry;

import edu.chemistry.EduMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class EduSounds {
    public static final SoundEvent BALLOON_POP = register("entity.balloon.pop");
    public static final SoundEvent ELEMENT_CONSTRUCTOR_OPEN = register("block.element_constructor.open");
    public static final SoundEvent LAB_PRODUCT = register("block.lab_table.product");
    public static final SoundEvent LAB_FIZZ = register("block.lab_table.fizz");
    public static final SoundEvent SPARKLER_ACTIVE = register("item.sparkler.active");
    public static final SoundEvent GLOW_STICK_USE = register("item.glow_stick.use");
    public static final SoundEvent ICE_BOMB_HIT = register("entity.ice_bomb.hit");
    public static final SoundEvent CAMERA_TAKE_PICTURE = register("item.camera.take_picture");

    private static SoundEvent register(String name) {
        ResourceLocation id = EduMod.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void init() {
    }

    private EduSounds() {
    }
}
