package edu.chemistry.client.legacy;

import edu.chemistry.EduMod;
import edu.chemistry.EduRules;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;

/**
 * Puts the Education Edition toggle in Legacy4J's (ReConsole's) world creation options, in the Game Options tab right
 * under Host Privileges, as one of its own tick boxes. It sets the same edu:education_features game rule as the button
 * on the normal Create New World screen. Called from WorldMoreOptionsScreenMixin; everything goes through reflection,
 * so nothing changes without Legacy4J.
 */
public final class LegacyWorldOptions {
    private static boolean failed;

    private LegacyWorldOptions() {
    }

    @SuppressWarnings("unchecked")
    public static void addEducationToggle(Object screen, Object hostPrivileges, CreateWorldScreen createWorld) {
        if (failed || createWorld == null) return;
        try {
            ClassLoader loader = LegacyWorldOptions.class.getClassLoader();
            Field gameList = Class.forName("wily.legacy.client.screen.WorldMoreOptionsScreen", false, loader).getDeclaredField("gameRenderables");
            gameList.setAccessible(true);
            Class<?> listClass = Class.forName("wily.legacy.client.screen.RenderableVList", false, loader);
            List<Renderable> renderables = (List<Renderable>) listClass.getField("renderables").get(gameList.get(screen));
            Class<?> tickBoxClass = Class.forName("wily.legacy.client.screen.TickBox", false, loader);
            Field selected = tickBoxClass.getField("selected");
            Method updateMessage = tickBoxClass.getMethod("updateMessage");
            Constructor<?> make = tickBoxClass.getConstructor(int.class, int.class, boolean.class, Function.class, Function.class, Consumer.class);

            WorldCreationUiState state = createWorld.getUiState();
            Component name = Component.translatable("createWorld.edu.education");
            Tooltip tooltip = Tooltip.create(Component.translatable("createWorld.edu.education.info"));
            Function<Boolean, Component> message = on -> name;
            Function<Boolean, Tooltip> tip = on -> tooltip;
            Consumer<Object> toggled = box -> {
                try {
                    state.getGameRules().set(EduRules.EDUCATION_FEATURES, selected.getBoolean(box), null);
                } catch (ReflectiveOperationException e) {
                    EduMod.LOG.warn("Couldn't read the Education Edition tick box", e);
                }
            };
            Object box = make.newInstance(0, 0, state.getGameRules().get(EduRules.EDUCATION_FEATURES), message, tip, toggled);

            // the menu's own game rule list (when it isn't using the console-style world options) already has the rule
            // under its raw name: one tick box for it is enough
            Component ruleName = Component.translatable(EduRules.EDUCATION_FEATURES.getDescriptionId());
            renderables.removeIf(r -> tickBoxClass.isInstance(r) && r instanceof AbstractWidget w && ruleName.equals(w.getMessage()));
            int at = renderables.indexOf(hostPrivileges);
            renderables.add(at >= 0 ? at + 1 : renderables.size(), (Renderable) box);

            // follow changes made elsewhere (the normal Create New World button, game rule edits)
            state.addListener(s -> {
                try {
                    boolean on = s.getGameRules().get(EduRules.EDUCATION_FEATURES);
                    if (selected.getBoolean(box) != on) {
                        selected.setBoolean(box, on);
                        updateMessage.invoke(box);
                    }
                } catch (ReflectiveOperationException | RuntimeException e) {
                    // the menu is gone or changed; the game rule itself is still right
                }
            });
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            failed = true;
            EduMod.LOG.warn("Couldn't add the Education Edition toggle to Legacy4J's world options", e);
        }
    }
}
