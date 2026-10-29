package edu.chemistry.fabric;

import edu.chemistry.EduMod;
import edu.chemistry.registry.EduRegistration;
import net.fabricmc.api.ModInitializer;

/** Fabric's entry point: the registrations, all at once (Fabric's registries stay open until the game starts). */
public class EduFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        EduMod.loader = EduMod.Loader.FABRIC;
        EduRegistration.registerAll();
    }
}
