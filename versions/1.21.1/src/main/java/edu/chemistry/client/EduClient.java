package edu.chemistry.client;

import edu.chemistry.menu.CompoundCreatorMenu;
import edu.chemistry.menu.ElementConstructorMenu;
import edu.chemistry.menu.LabTableMenu;
import edu.chemistry.menu.MaterialReducerMenu;
import edu.chemistry.registry.EduEntities;
import edu.chemistry.registry.EduMenus;
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
    @Override
    public void onInitializeClient() {
        registerRenderLayers();
        registerScreen(EduMenus.ELEMENT_CONSTRUCTOR, ElementConstructorScreen::new);
        registerScreen(EduMenus.COMPOUND_CREATOR, CompoundCreatorScreen::new);
        registerScreen(EduMenus.LAB_TABLE, LabTableScreen::new);
        registerScreen(EduMenus.MATERIAL_REDUCER, MaterialReducerScreen::new);

        registerRenderer(EduEntities.BALLOON, BalloonRenderer::new);
        registerRenderer(EduEntities.ICE_BOMB, ThrownItemRenderer::new);
        registerRenderer(EduEntities.UNDERWATER_TNT, TntRenderer::new);

        registerBlockEntityRenderer(edu.chemistry.registry.EduBlockEntities.CHALKBOARD, ChalkboardRenderer::new);
        registerRenderer(EduEntities.NPC, NpcRenderer::new);
        registerRenderer(EduEntities.TRIPOD_CAMERA, TripodCameraRenderer::new);
        registerRenderer(EduEntities.AGENT, AgentRenderer::new);
        edu.chemistry.photo.PhotoHooks.takePicture = (closeUp, from) -> edu.chemistry.client.photo.PictureTaker.request(closeUp, from);
        edu.chemistry.item.GuideBookItem.open = () -> Minecraft.getInstance().setScreen(new GuideBookScreen());
        edu.chemistry.photo.PhotoHooks.openPortfolio = () -> Minecraft.getInstance().setScreen(new edu.chemistry.client.photo.PortfolioScreen());
        edu.chemistry.photo.PhotoHooks.viewPhoto = stack -> Minecraft.getInstance().setScreen(new edu.chemistry.client.photo.PhotoViewScreen(stack));
        edu.chemistry.entity.NpcEntity.openScreen = npc -> Minecraft.getInstance().setScreen(new NpcDialogScreen(npc));
        edu.chemistry.block.ChalkboardBlock.openEditor = pos -> Minecraft.getInstance().setScreen(new ChalkboardScreen(pos));
    }

    /**
     * See-through blocks (glass, torches...). The newer builds work this out from the block's textures; 1.21.1 needs to
     * be told, so the asset generator lists them in assets/edu/render_layers.json.
     */
    private static void registerRenderLayers() {
        try (java.io.InputStream in = EduClient.class.getResourceAsStream("/assets/edu/render_layers.json")) {
            if (in == null) return;
            com.google.gson.JsonObject layers = com.google.gson.JsonParser.parseReader(
                new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            for (var e : layers.entrySet()) {
                net.minecraft.world.level.block.Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                    net.minecraft.resources.ResourceLocation.parse(e.getKey()));
                net.minecraft.client.renderer.RenderType type = e.getValue().getAsString().equals("translucent")
                    ? net.minecraft.client.renderer.RenderType.translucent() : net.minecraft.client.renderer.RenderType.cutout();
                net.minecraft.client.renderer.ItemBlockRenderTypes.TYPE_BY_BLOCK.put(block, type);
            }
        } catch (java.io.IOException | RuntimeException e) {
            edu.chemistry.EduMod.LOG.warn("Couldn't read the see-through block list", e);
        }
    }

    /** balloons in the inventory take their dye colour (layer 0 is the balloon, layer 1 its shine and string) */
    public static void registerItemColors(net.minecraft.client.color.item.ItemColors colors) {
        for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (item instanceof edu.chemistry.item.BalloonItem balloon) {
                int color = balloon.color().getTextureDiffuseColor() | 0xFF000000;
                colors.register((stack, layer) -> layer == 0 ? color : -1, item);
            }
        }
    }

    // 1.21.1: these registries are opened by the mod's access widener, so they're called directly (reflection by
    // name can't work with the intermediary names a real game runs with)
    private static <T extends net.minecraft.world.entity.Entity> void registerRenderer(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
        EntityRenderers.register(type, provider);
    }

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity>
    void registerBlockEntityRenderer(net.minecraft.world.level.block.entity.BlockEntityType<? extends T> type,
                                     net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider<T> provider) {
        net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(type, provider);
    }

    private static <M extends AbstractContainerMenu, U extends Screen & MenuAccess<M>> void registerScreen(MenuType<M> type, MenuScreens.ScreenConstructor<M, U> factory) {
        MenuScreens.register(type, factory);
    }
}
