package edu.chemistry.forge;

import edu.chemistry.client.EduClient;
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
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/** Forge's client setup: the renderers go in through Forge's event, the menu screens straight into the game's list. */
final class EduForgeClient {
    private EduForgeClient() {
    }

    static void init(BusGroup modBus) {
        EntityRenderersEvent.RegisterRenderers.BUS.addListener(event -> EduClient.registerRenderers(new EduClient.Renderers() {
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
        FMLClientSetupEvent.getBus(modBus).addListener(event -> event.enqueueWork(() -> {
            // Forge opens up the game's MenuScreens.register for this
            EduClient.registerScreens(new EduClient.Screens() {
                @Override
                @SuppressWarnings({"unchecked", "rawtypes"})
                public <M extends AbstractContainerMenu> void screen(MenuType<M> type, EduClient.ScreenFactory<M> factory) {
                    MenuScreens.register(type, (MenuScreens.ScreenConstructor) (menu, inventory, title) ->
                        factory.create((M) menu, inventory, title));
                }
            });
            EduClient.init();
        }));
    }
}
