package edu.chemistry.registry;

import edu.chemistry.EduMod;
import edu.chemistry.menu.CompoundCreatorMenu;
import edu.chemistry.menu.ElementConstructorMenu;
import edu.chemistry.menu.LabTableMenu;
import edu.chemistry.menu.MaterialReducerMenu;
import edu.chemistry.util.Reflect;
import java.lang.reflect.Constructor;
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

    @SuppressWarnings("unchecked")
    private static <T extends AbstractContainerMenu> MenuType<T> register(String name, BiFunction<Integer, Inventory, T> factory) {
        try {
            // MenuType's constructor and MenuSupplier are private in vanilla; this mod doesn't call Fabric API.
            Class<?> supplierClass = Reflect.cls(MenuType.class, "net.minecraft.world.inventory.MenuType$MenuSupplier");
            Object supplier = Reflect.implement(supplierClass, "create", args -> factory.apply((Integer) args[0], (Inventory) args[1]));
            Constructor<?> ctor = MenuType.class.getDeclaredConstructor(supplierClass, FeatureFlagSet.class);
            ctor.setAccessible(true);
            MenuType<T> type = (MenuType<T>) ctor.newInstance(supplier, FeatureFlags.VANILLA_SET);
            return Registry.register(BuiltInRegistries.MENU, EduMod.id(name), type);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Could not create menu type " + name, e);
        }
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
