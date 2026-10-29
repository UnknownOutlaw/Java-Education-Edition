package edu.chemistry;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The mod's id and log. Each mod loader has its own entry point (edu.chemistry.fabric, .neoforge and .forge), which
 * sets {@link #loader} first and then runs the registrations in {@link edu.chemistry.registry.EduRegistration}.
 */
public final class EduMod {
    public static final String MOD_ID = "edu";
    public static final Logger LOG = LoggerFactory.getLogger("Education Chemistry");

    public enum Loader { FABRIC, NEOFORGE, FORGE }

    /** the mod loader running the game */
    public static Loader loader = Loader.FABRIC;

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    private EduMod() {
    }
}
