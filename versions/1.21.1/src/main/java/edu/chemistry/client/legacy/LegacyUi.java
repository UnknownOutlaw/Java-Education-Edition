package edu.chemistry.client.legacy;

import edu.chemistry.EduMod;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import org.jspecify.annotations.Nullable;

/**
 * Talks to Legacy4J (the console-style UI that ReConsole is built on) when it's installed, without depending on it:
 * everything goes through reflection, so the mod works the same without it, and an older or newer Legacy4J that
 * doesn't match just means the station menus keep their normal look.
 */
public final class LegacyUi {
    public static final ResourceLocation PANEL = ResourceLocation.fromNamespaceAndPath("legacy", "tiles/small_panel");
    public static final ResourceLocation SD_PANEL = ResourceLocation.fromNamespaceAndPath("legacy", "tiles/panel");
    public static final ResourceLocation RECESS = ResourceLocation.fromNamespaceAndPath("legacy", "tiles/square_recessed_panel");
    public static final ResourceLocation ARROW = ResourceLocation.fromNamespaceAndPath("legacy", "container/arrow");
    public static final ResourceLocation SMALL_ARROW = ResourceLocation.fromNamespaceAndPath("legacy", "container/small_arrow");

    /** the slot display interface moved package between Legacy4J builds */
    private static final String[] DISPLAY_CLASSES = {"wily.legacy.client.control.navigation.LegacySlotDisplay",
        "wily.legacy.inventory.LegacySlotDisplay"};

    private static @Nullable Boolean installed;
    private static boolean checked;
    private static boolean present;
    private static Class<?> displayClass;
    private static Method override;
    private static Method getUIMode;
    private static Method isSD;
    private static @Nullable Object grayText;
    private static @Nullable Method colorGet;
    private static @Nullable Method sdFont;

    private LegacyUi() {
    }

    /** whether Legacy4J is installed at all, even a build whose console look can't be used here */
    public static boolean installed() {
        if (installed == null) {
            boolean found;
            try {
                Class.forName("wily.legacy.Legacy4J", false, LegacyUi.class.getClassLoader());
                found = true;
            } catch (ClassNotFoundException | LinkageError e) {
                found = false;
            }
            installed = found;
        }
        return installed;
    }

    /** whether Legacy4J is installed and the parts of it used here were found */
    public static boolean active() {
        if (!checked) {
            checked = true;
            try {
                ClassLoader loader = LegacyUi.class.getClassLoader();
                Class.forName("wily.legacy.Legacy4J", false, loader);
                for (String name : DISPLAY_CLASSES) {
                    try {
                        displayClass = Class.forName(name, false, loader);
                        break;
                    } catch (ClassNotFoundException ignored) {
                        // try the other package
                    }
                }
                if (displayClass == null) throw new ClassNotFoundException("LegacySlotDisplay");
                override = displayClass.getMethod("override", Slot.class, int.class, int.class, displayClass);
                Class<?> options = Class.forName("wily.legacy.client.LegacyOptions", false, loader);
                getUIMode = options.getMethod("getUIMode");
                isSD = getUIMode.getReturnType().getMethod("isSD");
                try {
                    Class<?> color = Class.forName("wily.legacy.client.CommonColor", false, loader);
                    grayText = color.getField("GRAY_TEXT").get(null);
                    colorGet = grayText.getClass().getMethod("get");
                } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                    grayText = null;
                }
                try {
                    sdFont = Class.forName("wily.legacy.util.client.LegacyFontUtil", false, loader).getMethod("applySDFont", Consumer.class);
                } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                    sdFont = null;
                }
                sd(); // make sure the UI mode can be read
                present = true;
                EduMod.LOG.info("Legacy4J found: station menus use its console layout");
            } catch (ClassNotFoundException e) {
                present = false;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                present = false;
                EduMod.LOG.warn("Legacy4J is installed but doesn't match what Education Edition expects; station menus keep their normal look", e);
            }
        }
        return present;
    }

    /** Legacy4J's small-screen layout (480p): 13 pixel slots instead of 21 */
    public static boolean sd() {
        try {
            return (Boolean) isSD.invoke(getUIMode.invoke(null));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return false;
        }
    }

    /** the dark gray Legacy4J uses for menu titles and labels (resource packs can change it) */
    public static int grayText() {
        if (grayText != null && colorGet != null) {
            try {
                return (Integer) colorGet.invoke(grayText);
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                grayText = null;
            }
        }
        return 0xFF323232;
    }

    /** draws text the way Legacy4J's menus do: in its smaller font on small screens */
    public static void labels(Consumer<Boolean> draw) {
        if (sdFont != null) {
            try {
                sdFont.invoke(null, draw);
                return;
            } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
                sdFont = null;
            }
        }
        draw.accept(sd());
    }

    /** the menu's background panel */
    public static void panel(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.blitSprite(sd() ? SD_PANEL : PANEL, x, y, w, h);
    }

    /** a sunken area inside the panel */
    public static void recess(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.blitSprite(RECESS, x, y, w, h);
    }

    /** the gray "goes to" arrow from the furnace and crafting menus */
    public static void arrow(GuiGraphics graphics, int x, int y, boolean sd) {
        if (sd) graphics.blitSprite(SMALL_ARROW, x, y, 16, 14);
        else graphics.blitSprite(ARROW, x, y, 33, 24);
    }

    /**
     * Moves a slot and gives it one of Legacy4J's slot frames of the given size. A warning slot gets the red frame (the
     * Material Reducer's input when the item can't be broken down).
     */
    public static void slot(Slot slot, int x, int y, int size, @Nullable BooleanSupplier warning) {
        Object display = display(size, warning);
        try {
            override.invoke(null, slot, x, y, display);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Couldn't place slot", e.getCause() != null ? e.getCause() : e);
        }
    }

    /** a Legacy4J slot display: the frame size, and whether it's the red warning frame */
    public static Object display(int size, @Nullable BooleanSupplier warning) {
        return Proxy.newProxyInstance(LegacyUi.class.getClassLoader(), new Class<?>[] {displayClass}, handler(size, warning));
    }

    private static InvocationHandler handler(int size, @Nullable BooleanSupplier warning) {
        return (proxy, method, args) -> switch (method.getName()) {
            case "getWidth", "getHeight" -> size;
            case "isWarning" -> warning != null && warning.getAsBoolean();
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            case "toString" -> "edu-slot-display(" + size + ")";
            default -> method.isDefault() ? InvocationHandler.invokeDefault(proxy, method, args) : null;
        };
    }
}
