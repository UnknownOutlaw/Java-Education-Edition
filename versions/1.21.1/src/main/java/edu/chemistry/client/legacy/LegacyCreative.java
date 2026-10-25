package edu.chemistry.client.legacy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import edu.chemistry.EduMod;
import edu.chemistry.EduRules;
import edu.chemistry.registry.EduTabs;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/**
 * Legacy4J's console creative menu builds its tabs from its own tab list, and only shows other mods' tabs after its
 * eight console tabs. This puts the Education Edition categories (Education, Lab, Elements, Compounds, Products) into
 * that list as tabs of their own, only while Education Edition is turned on, and moves the single Education Edition tab
 * out of the way (its items stay in search). Everything goes through reflection, so nothing changes without Legacy4J.
 */
public final class LegacyCreative {
    private static boolean failed;
    private static boolean ready;
    private static Map<Object, Object> listingMap;
    private static Object consoleCreativeOption;
    private static Method optionGet;
    private static final Map<ResourceLocation, Object> LISTINGS = new LinkedHashMap<>();

    private LegacyCreative() {
    }

    /** keeps Legacy4J's tab list in step with the world's Education Edition setting; called every frame */
    @SuppressWarnings("unchecked")
    public static void sync() {
        if (failed || !LegacyUi.active()) return;
        try {
            if (!ready) setup();
            boolean console = (Boolean) optionGet.invoke(consoleCreativeOption);
            if (console != EduTabs.consoleCreative) {
                EduTabs.consoleCreative = console;
                EduTabs.refresh();
            }
            ResourceLocation first = LISTINGS.keySet().iterator().next();
            boolean shown = listingMap.containsKey(first);
            // the list is rebuilt from resource packs on every reload, so this puts the tabs back afterwards too
            if (EduRules.clientEnabled && !shown) LISTINGS.forEach(listingMap::put);
            else if (!EduRules.clientEnabled && shown) LISTINGS.keySet().forEach(listingMap::remove);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            failed = true;
            EduTabs.consoleCreative = false;
            EduTabs.refresh();
            EduMod.LOG.warn("Couldn't add the Education Edition tabs to Legacy4J's creative menu", e);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void setup() throws ReflectiveOperationException {
        ClassLoader loader = LegacyCreative.class.getClassLoader();
        Object manager = Class.forName("wily.legacy.Legacy4JClient", false, loader).getField("legacyCreativeListingManager").get(null);
        listingMap = (Map<Object, Object>) manager.getClass().getMethod("map").invoke(manager);
        consoleCreativeOption = Class.forName("wily.legacy.client.LegacyOptions", false, loader).getField("legacyCreativeTab").get(null);
        optionGet = Class.forName("wily.factoryapi.base.config.FactoryConfig", false, loader).getMethod("get");
        Codec codec = (Codec) Class.forName("wily.legacy.client.LegacyCreativeTabListing", false, loader).getField("CODEC").get(null);
        DynamicOps<JsonElement> ops = JsonOps.INSTANCE;
        try {
            ops = (DynamicOps<JsonElement>) Class.forName("wily.factoryapi.util.DynamicUtil", false, loader)
                .getMethod("getActualRegistryOps", DynamicOps.class).invoke(null, JsonOps.INSTANCE);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            // plain JSON is enough: the items are only looked up when the menu opens
        }
        for (EduTabs.Category category : EduTabs.CATEGORIES) {
            ResourceLocation id = EduMod.id("console_" + category.key());
            JsonObject json = new JsonObject();
            json.addProperty("id", id.toString());
            JsonObject name = new JsonObject();
            name.addProperty("translate", "itemGroup.edu.category." + category.key());
            json.add("name", name);
            JsonObject icon = new JsonObject();
            icon.addProperty("type", "item");
            icon.addProperty("value", BuiltInRegistries.ITEM.getKey(category.icon().get()).toString());
            json.add("icon", icon);
            JsonArray items = new JsonArray();
            for (Item item : category.items().get()) items.add(BuiltInRegistries.ITEM.getKey(item).toString());
            json.add("listing", items);
            Object listing = codec.parse(ops, json).getOrThrow();
            LISTINGS.put(id, listing);
        }
        ready = true;
    }
}
