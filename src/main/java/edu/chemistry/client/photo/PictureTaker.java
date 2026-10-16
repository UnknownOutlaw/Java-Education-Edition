package edu.chemistry.client.photo;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

/**
 * Takes a camera picture: for one frame it hides the HUD (and, for a tripod camera, looks through the tripod's lens),
 * grabs the frame and adds it to the portfolio. Sneaking with the camera takes a close-up (the middle of the view).
 */
public final class PictureTaker {
    /** stored pictures are scaled down to this width */
    private static final int WIDTH = 640;
    private static int stage;
    private static boolean closeUp;
    private static @Nullable Entity camera;
    private static boolean savedHideGui;
    private static @Nullable CameraType savedCameraType;
    private static @Nullable Entity savedCameraEntity;

    public static void request(boolean closeUpShot, Entity from) {
        if (stage != 0) return;
        closeUp = closeUpShot;
        camera = from;
        stage = 1;
    }

    /** true while the frame being drawn is looking through the tripod camera (it is never shown on screen) */
    private static boolean hidingFrame;

    /** start of a frame: frames drawn for the picture are kept off the screen */
    public static void beforeFrame() {
        hidingFrame = stage >= 2;
    }

    /** whether this frame should be kept off the screen (the picture's own frames) */
    public static boolean hideThisFrame() {
        return hidingFrame;
    }

    /**
     * End of each frame. Once a picture is asked for, the shot is set up (HUD hidden, looking through the tripod
     * camera's lens if it's a tripod picture), a whole frame is drawn that way, then that frame is grabbed and
     * everything put back. Setting up a frame ahead makes sure the camera really moved to the lens before the shot.
     */
    public static void afterFrame() {
        if (stage == 0) return;
        Minecraft mc = Minecraft.getInstance();
        if (stage == 1) {
            savedHideGui = mc.options.hideGui;
            savedCameraType = mc.options.getCameraType();
            savedCameraEntity = mc.getCameraEntity();
            mc.options.hideGui = true;
            if (camera != null && camera != mc.player && !camera.isRemoved()) mc.setCameraEntity(camera);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            stage = 2;
            return;
        }
        if (stage == 2) {
            // one full frame drawn from the new viewpoint; grab the next one
            stage = 3;
            return;
        }
        stage = 0;
        boolean zoom = closeUp;
        Screenshot.takeScreenshot(mc.getMainRenderTarget(), image -> save(image, zoom));
        mc.options.hideGui = savedHideGui;
        if (savedCameraType != null) mc.options.setCameraType(savedCameraType);
        mc.setCameraEntity(savedCameraEntity != null ? savedCameraEntity : mc.player);
        camera = null;
        PhotoToast.flash();
    }

    private static void save(NativeImage frame, boolean zoom) {
        try (frame) {
            int sx = 0;
            int sy = 0;
            int sw = frame.getWidth();
            int sh = frame.getHeight();
            if (zoom) {
                sx = sw / 4;
                sy = sh / 4;
                sw /= 2;
                sh /= 2;
            }
            int w = Math.min(WIDTH, sw);
            int h = Math.max(1, Math.round(sh * (w / (float) sw)));
            NativeImage out = new NativeImage(w, h, false);
            frame.resizeSubRectTo(sx, sy, sw, sh, out);
            // the frame buffer's alpha isn't meaningful: make the picture fully opaque
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) out.setPixel(x, y, out.getPixel(x, y) | 0xFF000000);
            }
            PortfolioStore store = PortfolioStore.current();
            PortfolioStore.Photo p = store.add(out, "");
            if (p != null) PhotoToast.show(store.texture(p));
        }
    }

    private PictureTaker() {
    }
}
