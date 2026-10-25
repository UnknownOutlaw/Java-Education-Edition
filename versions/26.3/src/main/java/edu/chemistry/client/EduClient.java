package edu.chemistry.client;

import edu.chemistry.EduMod;
import edu.chemistry.menu.CompoundCreatorMenu;
import edu.chemistry.menu.ElementConstructorMenu;
import edu.chemistry.menu.LabTableMenu;
import edu.chemistry.menu.MaterialReducerMenu;
import edu.chemistry.registry.EduEntities;
import edu.chemistry.registry.EduMenus;
import edu.chemistry.util.Reflect;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Function;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.renderer.entity.TntRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public class EduClient implements ClientModInitializer {
    @FunctionalInterface
    interface ScreenFactory<M extends AbstractContainerMenu> {
        Screen create(M menu, Inventory inventory, Component title);
    }

    @Override
    public void onInitializeClient() {
        registerScreen(EduMenus.ELEMENT_CONSTRUCTOR, (ScreenFactory<ElementConstructorMenu>) ElementConstructorScreen::new);
        registerScreen(EduMenus.COMPOUND_CREATOR, (ScreenFactory<CompoundCreatorMenu>) CompoundCreatorScreen::new);
        registerScreen(EduMenus.LAB_TABLE, (ScreenFactory<LabTableMenu>) LabTableScreen::new);
        registerScreen(EduMenus.MATERIAL_REDUCER, (ScreenFactory<MaterialReducerMenu>) MaterialReducerScreen::new);

        registerRenderer(EduEntities.BALLOON, BalloonRenderer::new);
        registerRenderer(EduEntities.ICE_BOMB, ThrownItemRenderer::new);
        registerRenderer(EduEntities.UNDERWATER_TNT, TntRenderer::new);

        registerBlockEntityRenderer(edu.chemistry.registry.EduBlockEntities.CHALKBOARD, ChalkboardRenderer::new);
        registerRenderer(EduEntities.NPC, NpcRenderer::new);
        registerRenderer(EduEntities.TRIPOD_CAMERA, TripodCameraRenderer::new);
        registerRenderer(EduEntities.AGENT, AgentRenderer::new);
        edu.chemistry.photo.PhotoHooks.takePicture = (closeUp, from) -> edu.chemistry.client.photo.PictureTaker.request(closeUp, from);
        edu.chemistry.item.GuideBookItem.open = () -> Minecraft.getInstance().gui.setScreen(new GuideBookScreen());
        edu.chemistry.photo.PhotoHooks.openPortfolio = () -> Minecraft.getInstance().gui.setScreen(new edu.chemistry.client.photo.PortfolioScreen());
        edu.chemistry.photo.PhotoHooks.viewPhoto = stack -> Minecraft.getInstance().gui.setScreen(new edu.chemistry.client.photo.PhotoViewScreen(stack));
        edu.chemistry.entity.NpcEntity.openScreen = npc -> Minecraft.getInstance().gui.setScreen(new NpcDialogScreen(npc));
        edu.chemistry.block.ChalkboardBlock.openEditor = pos -> Minecraft.getInstance().gui.setScreen(new ChalkboardScreen(pos));
    }

    private static <T extends net.minecraft.world.entity.Entity> void registerRenderer(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
        Method register = Reflect.method(EntityRenderers.class, "register", EntityType.class, EntityRendererProvider.class);
        Reflect.invoke(register, null, type, provider);
    }

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity, S extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState>
    void registerBlockEntityRenderer(net.minecraft.world.level.block.entity.BlockEntityType<? extends T> type,
                                     net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider<T, S> provider) {
        Method register = Reflect.method(net.minecraft.client.renderer.blockentity.BlockEntityRenderers.class, "register",
            net.minecraft.world.level.block.entity.BlockEntityType.class, net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.class);
        Reflect.invoke(register, null, type, provider);
    }

    /** MenuScreens.register and its ScreenConstructor interface are private in vanilla. */
    @SuppressWarnings("unchecked")
    private static <M extends AbstractContainerMenu> void registerScreen(MenuType<M> type, ScreenFactory<M> factory) {
        try {
            Class<?> ctorClass = Reflect.cls(MenuScreens.class, "net.minecraft.client.gui.screens.MenuScreens$ScreenConstructor");
            Function<Object[], Object> create = args -> factory.create((M) args[0], (Inventory) args[1], (Component) args[2]);
            Function<Object[], Object> fromPacket = args -> {
                Component title = (Component) args[0];
                MenuType<M> menuType = (MenuType<M>) args[1];
                Minecraft minecraft = (Minecraft) args[2];
                int containerId = (Integer) args[3];
                Inventory inv = minecraft.player.getInventory();
                Screen screen = factory.create(menuType.create(containerId, inv), inv, title);
                minecraft.player.containerMenu = ((MenuAccess<?>) screen).getMenu();
                minecraft.gui.setScreen(screen);
                return null;
            };
            Object constructor = Reflect.implement(ctorClass, Map.of("create", create, "fromPacket", fromPacket));
            Method register = Reflect.method(MenuScreens.class, "register", MenuType.class, ctorClass);
            Reflect.invoke(register, null, type, constructor);
        } catch (RuntimeException e) {
            EduMod.LOG.error("Could not register screen for {}", type, e);
        }
    }
}
