package edu.chemistry.client;

import edu.chemistry.menu.CompoundCreatorMenu;
import edu.chemistry.menu.ElementConstructorMenu;
import edu.chemistry.menu.LabTableMenu;
import edu.chemistry.menu.MaterialReducerMenu;
import edu.chemistry.registry.EduEntities;
import edu.chemistry.registry.EduMenus;
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

/**
 * The client's setup. Each mod loader puts the menu screens, renderers and see-through blocks in place its own way
 * (through {@link Screens} and {@link Renderers}: Fabric puts them straight into the game's own lists with
 * {@link #VANILLA}), then runs {@link #init()}.
 */
public final class EduClient {
    @FunctionalInterface
    public interface ScreenFactory<M extends AbstractContainerMenu> {
        Screen create(M menu, Inventory inventory, Component title);
    }

    /** puts a menu's screen in place */
    public interface Screens {
        <M extends AbstractContainerMenu> void screen(MenuType<M> type, ScreenFactory<M> factory);
    }

    /** puts an entity's or block entity's renderer in place */
    public interface Renderers {
        <T extends net.minecraft.world.entity.Entity> void entity(EntityType<? extends T> type, EntityRendererProvider<T> provider);

        <T extends net.minecraft.world.level.block.entity.BlockEntity> void blockEntity(
            net.minecraft.world.level.block.entity.BlockEntityType<? extends T> type,
            net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider<T> provider);
    }

    /** the game's own lists (opened by the mod's access widener on Fabric) */
    public static final Vanilla VANILLA = new Vanilla();

    public static final class Vanilla implements Screens, Renderers {
        private Vanilla() {
        }

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public <M extends AbstractContainerMenu> void screen(MenuType<M> type, ScreenFactory<M> factory) {
            MenuScreens.register(type, (MenuScreens.ScreenConstructor) (menu, inventory, title) -> factory.create((M) menu, inventory, title));
        }

        @Override
        public <T extends net.minecraft.world.entity.Entity> void entity(EntityType<? extends T> type, EntityRendererProvider<T> provider) {
            EntityRenderers.register(type, provider);
        }

        @Override
        public <T extends net.minecraft.world.level.block.entity.BlockEntity> void blockEntity(
            net.minecraft.world.level.block.entity.BlockEntityType<? extends T> type,
            net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider<T> provider) {
            net.minecraft.client.renderer.blockentity.BlockEntityRenderers.register(type, provider);
        }
    }

    private EduClient() {
    }

    public static void registerScreens(Screens screens) {
        screens.screen(EduMenus.ELEMENT_CONSTRUCTOR, (ScreenFactory<ElementConstructorMenu>) ElementConstructorScreen::new);
        screens.screen(EduMenus.COMPOUND_CREATOR, (ScreenFactory<CompoundCreatorMenu>) CompoundCreatorScreen::new);
        screens.screen(EduMenus.LAB_TABLE, (ScreenFactory<LabTableMenu>) LabTableScreen::new);
        screens.screen(EduMenus.MATERIAL_REDUCER, (ScreenFactory<MaterialReducerMenu>) MaterialReducerScreen::new);
    }

    public static void registerRenderers(Renderers renderers) {
        renderers.entity(EduEntities.BALLOON, BalloonRenderer::new);
        renderers.entity(EduEntities.ICE_BOMB, ThrownItemRenderer::new);
        renderers.entity(EduEntities.UNDERWATER_TNT, TntRenderer::new);

        renderers.blockEntity(edu.chemistry.registry.EduBlockEntities.CHALKBOARD, ChalkboardRenderer::new);
        renderers.entity(EduEntities.NPC, NpcRenderer::new);
        renderers.entity(EduEntities.TRIPOD_CAMERA, TripodCameraRenderer::new);
        renderers.entity(EduEntities.AGENT, AgentRenderer::new);
    }

    /** the rest of the client's setup: what the common code calls on the client */
    public static void init() {
        edu.chemistry.network.EduNetwork.clientHandler = payload -> {
            if (payload instanceof edu.chemistry.code.CodePayload code) edu.chemistry.client.code.CodeBuilderClient.fromServer(code.json());
            if (payload instanceof edu.chemistry.network.EduSyncPayload sync) edu.chemistry.EduRules.clientSync(sync.name(), sync.value());
        };
        edu.chemistry.photo.PhotoHooks.takePicture = (closeUp, from) -> edu.chemistry.client.photo.PictureTaker.request(closeUp, from);
        edu.chemistry.item.GuideBookItem.open = () -> Minecraft.getInstance().setScreen(new GuideBookScreen());
        edu.chemistry.photo.PhotoHooks.openPortfolio = () -> Minecraft.getInstance().setScreen(new edu.chemistry.client.photo.PortfolioScreen());
        edu.chemistry.photo.PhotoHooks.viewPhoto = stack -> Minecraft.getInstance().setScreen(new edu.chemistry.client.photo.PhotoViewScreen(stack));
        edu.chemistry.entity.NpcEntity.openScreen = npc -> Minecraft.getInstance().setScreen(new NpcDialogScreen(npc));
        edu.chemistry.block.ChalkboardBlock.openEditor = pos -> Minecraft.getInstance().setScreen(new ChalkboardScreen(pos));
    }

    /**
     * See-through blocks (glass, torches...). The newer builds work this out from the block's textures; 1.21.1 needs to
     * be told, so the asset generator lists them in assets/edu/render_layers.json. Fabric puts them straight in the game's
     * list; NeoForge and Forge keep their own, which they fill through ItemBlockRenderTypes.setRenderLayer.
     */
    public static void registerRenderLayers(java.util.function.BiConsumer<net.minecraft.world.level.block.Block, net.minecraft.client.renderer.RenderType> setLayer) {
        try (java.io.InputStream in = EduClient.class.getResourceAsStream("/assets/edu/render_layers.json")) {
            if (in == null) return;
            com.google.gson.JsonObject layers = com.google.gson.JsonParser.parseReader(
                new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            for (var e : layers.entrySet()) {
                net.minecraft.world.level.block.Block block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(
                    net.minecraft.resources.ResourceLocation.parse(e.getKey()));
                net.minecraft.client.renderer.RenderType type = e.getValue().getAsString().equals("translucent")
                    ? net.minecraft.client.renderer.RenderType.translucent() : net.minecraft.client.renderer.RenderType.cutout();
                setLayer.accept(block, type);
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
}
