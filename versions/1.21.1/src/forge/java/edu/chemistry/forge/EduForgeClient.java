package edu.chemistry.forge;

import edu.chemistry.client.EduClient;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Forge's client setup: the renderers go in through Forge's event, the menu screens straight into the game's list
 * (Forge opens it up), and the see-through blocks through Forge's own list of them.
 */
final class EduForgeClient {
    private EduForgeClient() {
    }

    @SuppressWarnings("deprecation")
    static void init(IEventBus modBus) {
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> EduClient.registerRenderers(new EduClient.Renderers() {
            @Override
            public <T extends Entity> void entity(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
                event.registerEntityRenderer(type, provider);
            }

            @Override
            public <T extends BlockEntity> void blockEntity(BlockEntityType<? extends T> type, BlockEntityRendererProvider<T> provider) {
                event.registerBlockEntityRenderer(type, provider);
            }
        }));
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(() -> {
            EduClient.registerRenderLayers(ItemBlockRenderTypes::setRenderLayer);
            EduClient.registerScreens(EduClient.VANILLA);
            EduClient.init();
        }));
    }
}
