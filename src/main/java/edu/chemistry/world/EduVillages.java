package edu.chemistry.world;

import edu.chemistry.EduMod;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import edu.chemistry.EduRules;
import net.minecraft.server.MinecraftServer;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.structure.pools.SinglePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * Adds the Chemist's lab (data/edu/structure/village/<biome>/houses/chemist_lab.nbt) to each village's house pool when
 * a world's registries load, so new villages can build one. Done in code rather than by overriding vanilla's pool files,
 * so it keeps working alongside other mods and datapacks that change the villages.
 *
 * The labs are Education Edition content, so villages only build them while the world has edu:education_features on:
 * StructureTemplatePoolMixin leaves them out of the pool's picks otherwise (checked as each village is laid out, so
 * switching the rule on or off affects villages generated from then on).
 */
public final class EduVillages {
    public static final List<String> BIOMES = List.of("plains", "desert", "savanna", "snowy", "taiga");
    /** like vanilla's common small houses */
    public static final int WEIGHT = 2;

    /** the lab elements added to this world's pools */
    private static final Set<StructurePoolElement> LABS = ConcurrentHashMap.newKeySet();
    /** the running server, whose game rules decide whether labs can be built */
    private static volatile MinecraftServer server;

    private EduVillages() {
    }

    public static void addHouses(MinecraftServer server) {
        EduVillages.server = server;
        LABS.clear();
        addHouses(server.registryAccess());
    }

    /** whether villages may build labs right now: only while Education Edition is switched on in the world */
    public static boolean labsAllowed() {
        MinecraftServer s = server;
        if (s == null) return false;
        try {
            return s.getGameRules().get(EduRules.EDUCATION_FEATURES);
        } catch (RuntimeException e) {
            return false;  // rules not readable yet: the rule is off by default
        }
    }

    public static boolean isLab(StructurePoolElement element) {
        return LABS.contains(element);
    }

    public static boolean containsLab(List<StructurePoolElement> elements) {
        if (LABS.isEmpty()) return false;
        for (StructurePoolElement e : elements) {
            if (LABS.contains(e)) return true;
        }
        return false;
    }

    /** the list without any labs (the same list when it has none) */
    public static List<StructurePoolElement> withoutLabs(List<StructurePoolElement> elements) {
        if (!containsLab(elements)) return elements;
        List<StructurePoolElement> rest = new ArrayList<>(elements.size());
        for (StructurePoolElement e : elements) {
            if (!LABS.contains(e)) rest.add(e);
        }
        return rest;
    }

    public static void addHouses(RegistryAccess access) {
        Registry<StructureTemplatePool> pools = access.lookupOrThrow(Registries.TEMPLATE_POOL);
        for (String biome : BIOMES) {
            StructureTemplatePool pool = pools.getValue(Identifier.withDefaultNamespace("village/" + biome + "/houses"));
            if (pool == null) continue;  // a datapack removed it
            addTo(pool, EduMod.MOD_ID + ":village/" + biome + "/houses/chemist_lab", WEIGHT);
        }
    }

    /** the jigsaw picks from the pool's expanded list, where each element appears once per point of weight */
    @SuppressWarnings("unchecked")
    public static void addTo(StructureTemplatePool pool, String location, int weight) {
        try {
            Field f = StructureTemplatePool.class.getDeclaredField("templates");
            f.setAccessible(true);
            ObjectArrayList<StructurePoolElement> templates = (ObjectArrayList<StructurePoolElement>) f.get(pool);
            for (StructurePoolElement e : templates) {
                if (e instanceof SinglePoolElement single && single.getTemplateLocation().toString().equals(location)) {
                    LABS.add(e);  // already added
                    return;
                }
            }
            StructurePoolElement lab = StructurePoolElement.legacy(location).apply(StructureTemplatePool.Projection.RIGID);
            for (int i = 0; i < weight; i++) templates.add(lab);
            LABS.add(lab);
        } catch (ReflectiveOperationException e) {
            EduMod.LOG.error("Couldn't add {} to villages", location, e);
        }
    }
}
