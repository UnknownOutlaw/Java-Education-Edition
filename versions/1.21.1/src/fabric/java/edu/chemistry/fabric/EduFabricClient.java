package edu.chemistry.fabric;

import edu.chemistry.client.EduClient;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;

/** Fabric's client entry point: see-through blocks, screens and renderers go straight into the game's own lists. */
public class EduFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // the list is opened by the mod's access widener
        EduClient.registerRenderLayers(ItemBlockRenderTypes.TYPE_BY_BLOCK::put);
        EduClient.registerScreens(EduClient.VANILLA);
        EduClient.registerRenderers(EduClient.VANILLA);
        EduClient.init();
    }
}
