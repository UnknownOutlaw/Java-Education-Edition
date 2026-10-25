package edu.chemistry.registry;

import edu.chemistry.EduMod;
import edu.chemistry.menu.CompoundCreatorMenu;
import edu.chemistry.menu.ElementConstructorMenu;
import edu.chemistry.menu.LabTableMenu;
import edu.chemistry.menu.MaterialReducerMenu;
import java.util.function.BiFunction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public final class EduMenus {
    public static MenuType<ElementConstructorMenu> ELEMENT_CONSTRUCTOR;
    public static MenuType<CompoundCreatorMenu> COMPOUND_CREATOR;
    public static MenuType<LabTableMenu> LAB_TABLE;
    public static MenuType<MaterialReducerMenu> MATERIAL_REDUCER;

    /** MenuType's constructor is private in vanilla; the mod's access widener opens it (no Fabric API needed) */
    private static <T extends AbstractContainerMenu> MenuType<T> register(String name, BiFunction<Integer, Inventory, T> factory) {
        return Registry.register(BuiltInRegistries.MENU, EduMod.id(name), new MenuType<T>(factory::apply, FeatureFlags.VANILLA_SET));
    }

    public static void init() {
        ELEMENT_CONSTRUCTOR = register("element_constructor", ElementConstructorMenu::new);
        COMPOUND_CREATOR = register("compound_creator", CompoundCreatorMenu::new);
        LAB_TABLE = register("lab_table", LabTableMenu::new);
        MATERIAL_REDUCER = register("material_reducer", MaterialReducerMenu::new);
    }

    private EduMenus() {
    }
}
