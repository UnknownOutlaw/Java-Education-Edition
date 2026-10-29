package edu.chemistry.fabric;

import edu.chemistry.client.EduClient;
import net.fabricmc.api.ClientModInitializer;

/** Fabric's client entry point: screens and renderers go straight into the game's own lists. */
public class EduFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EduClient.registerScreens(EduClient.VANILLA);
        EduClient.registerRenderers(EduClient.VANILLA);
        EduClient.init();
    }
}
