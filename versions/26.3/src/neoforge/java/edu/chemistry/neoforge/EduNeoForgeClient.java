package edu.chemistry.neoforge;

import edu.chemistry.client.EduClient;
import edu.chemistry.code.CodePayload;
import edu.chemistry.network.EduNetwork;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;

/** NeoForge's client setup: Code Builder's packet handler, the menu screens and the renderers go in through NeoForge's events. */
final class EduNeoForgeClient {
    private EduNeoForgeClient() {
    }

    static void init(IEventBus modBus) {
        modBus.addListener((RegisterClientPayloadHandlersEvent event) ->
            event.register(CodePayload.TYPE, HandlerThread.MAIN, (payload, context) -> EduNetwork.clientHandler.accept(payload)));
        modBus.addListener((RegisterMenuScreensEvent event) -> EduClient.registerScreens(new EduClient.Screens() {
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public <M extends AbstractContainerMenu> void screen(MenuType<M> type, EduClient.ScreenFactory<M> factory) {
                event.register(type, (MenuScreens.ScreenConstructor) (menu, inventory, title) ->
                    factory.create((M) menu, inventory, title));
            }
        }));
        modBus.addListener((EntityRenderersEvent.RegisterRenderers event) -> EduClient.registerRenderers(new EduClient.Renderers() {
            @Override
            public <T extends Entity> void entity(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
                event.registerEntityRenderer(type, provider);
            }

            @Override
            public <T extends BlockEntity, S extends BlockEntityRenderState> void blockEntity(BlockEntityType<? extends T> type,
                                                                                           BlockEntityRendererProvider<T, S> provider) {
                event.registerBlockEntityRenderer(type, provider);
            }
        }));
        modBus.addListener((FMLClientSetupEvent event) -> event.enqueueWork(EduClient::init));
    }
}
