package edu.chemistry.client.legacy;

import edu.chemistry.EduMod;
import edu.chemistry.client.ControlTips;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import net.minecraft.client.OptionInstance;

/**
 * Puts the Control Tips toggle in Legacy4J's (ReConsole's) Key Binds menu, styled like the menu's other toggles.
 * Called from LegacyKeyMappingScreenMixin while the menu builds its list; everything goes through reflection, so
 * nothing changes without Legacy4J.
 */
public final class LegacySettings {
    private static boolean failed;
    private static Object configs;
    private static Method getList;
    private static Method addOptions;

    private LegacySettings() {
    }

    /** adds the toggle to the end of the menu's list so far */
    public static void addToKeyBinds(Object screen) {
        if (failed) return;
        try {
            if (configs == null) setup();
            addOptions.invoke(getList.invoke(screen), configs);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            failed = true;
            EduMod.LOG.warn("Couldn't add Control Tips to Legacy4J's Key Binds menu", e);
        }
    }

    private static void setup() throws ReflectiveOperationException {
        ClassLoader loader = LegacySettings.class.getClassLoader();
        ControlTips.ensureLoaded();
        Object config = Class.forName("wily.legacy.client.LegacyOptions", false, loader).getMethod("of", OptionInstance.class)
            .invoke(null, ControlTips.OPTION);
        Class<?> configClass = Class.forName("wily.factoryapi.base.config.FactoryConfig", false, loader);
        Object array = Array.newInstance(configClass, 1);
        Array.set(array, 0, config);
        getList = Class.forName("wily.legacy.client.screen.RenderableVList$Access", false, loader).getMethod("getRenderableVList");
        addOptions = Class.forName("wily.legacy.client.screen.RenderableVList", false, loader).getMethod("addOptions", array.getClass());
        configs = array;
    }
}
