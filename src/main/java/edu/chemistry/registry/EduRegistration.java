package edu.chemistry.registry;

import edu.chemistry.EduMod;
import edu.chemistry.EduRules;
import edu.chemistry.agent.Agents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;

/**
 * Everything the mod adds to the game's registries, one step per registry, in the order Fabric runs them. NeoForge and
 * Forge run each step in their registration event for that registry instead (Forge only lets a registry be added to
 * during its own event), so a step only adds to its own registry.
 */
public final class EduRegistration {
    private record Step(ResourceKey<? extends Registry<?>> registry, Runnable run) {
    }

    private static final List<Step> STEPS = List.of(
        new Step(Registries.GAME_RULE, EduRules::init),
        new Step(Registries.SOUND_EVENT, EduSounds::init),
        new Step(Registries.BLOCK, EduBlocks::init),
        new Step(Registries.BLOCK_ENTITY_TYPE, EduBlockEntities::init),
        new Step(Registries.ITEM, EduItems::init),
        new Step(Registries.POINT_OF_INTEREST_TYPE, EduVillagers::initJobSite),
        new Step(Registries.VILLAGER_PROFESSION, EduVillagers::initProfession),
        new Step(Registries.ENTITY_TYPE, EduEntities::init),
        new Step(Registries.TICKET_TYPE, Agents::init),
        new Step(Registries.MENU, EduMenus::init),
        new Step(Registries.CREATIVE_MODE_TAB, EduTabs::init),
        new Step(Registries.RECIPE_TYPE, EduRecipes::initTypes),
        new Step(Registries.RECIPE_SERIALIZER, EduRecipes::initSerializers),
        new Step(Registries.RECIPE_BOOK_CATEGORY, EduRecipes::initCategories));

    private static final List<Step> DONE = new ArrayList<>();

    private EduRegistration() {
    }

    /** Fabric: everything, in order */
    public static void registerAll() {
        for (Step step : STEPS) run(step);
    }

    /** NeoForge and Forge: the steps for the registry whose registration event is running */
    public static void register(ResourceKey<? extends Registry<?>> registry) {
        for (Step step : STEPS) {
            if (step.registry().equals(registry)) run(step);
        }
    }

    /** after the registration events: stops the game if a registry never had its event */
    public static void checkDone() {
        List<String> missed = STEPS.stream().filter(s -> !DONE.contains(s)).map(s -> s.registry().identifier().toString()).toList();
        if (!missed.isEmpty()) throw new IllegalStateException("Education Edition wasn't registered in: " + missed);
    }

    private static void run(Step step) {
        if (DONE.contains(step)) return;
        step.run().run();
        DONE.add(step);
        if (DONE.size() == STEPS.size()) {
            EduMod.LOG.info("Education Edition chemistry loaded: {} elements, {} compounds", 118, edu.chemistry.data.ChemData.COMPOUND_IDS.length);
        }
    }
}
