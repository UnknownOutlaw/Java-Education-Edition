package edu.chemistry.client.photo;

import edu.chemistry.client.GuiCompat;
import net.minecraft.Util;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;

/**
 * What you see when a picture is taken: Bedrock's screenshot flash (the screen goes white and fades out, easing
 * in_expo) and a small copy of the picture, framed like the portfolio's photos, that shows in the corner for two
 * seconds and fades away.
 */
public final class PhotoToast {
    private static final long FLASH_MS = 500L;
    private static final long SHOW_MS = 2000L;
    private static final long FADE_MS = 400L;
    private static long flashStart = -1L;
    private static long shownAt = -1L;
    private static @Nullable ResourceLocation picture;

    public static void flash() {
        flashStart = Util.getMillis();
    }

    public static void show(@Nullable ResourceLocation tex) {
        if (tex == null) return;
        picture = tex;
        shownAt = Util.getMillis();
    }

    private static int alpha(float a) {
        return Math.max(0, Math.min(255, Math.round(a * 255.0F)));
    }

    public static void render(GuiGraphics g, int screenW, int screenH) {
        edu.chemistry.client.GuiCompat.blend();
        long now = Util.getMillis();
        if (picture != null && shownAt >= 0L) {
            long t = now - shownAt;
            if (t > SHOW_MS) {
                picture = null;
            } else {
                float a = t > SHOW_MS - FADE_MS ? (SHOW_MS - t) / (float) FADE_MS : 1.0F;
                int[] size = PortfolioStore.size(picture);
                int w = Math.max(80, screenW / 5);
                int h = Math.round(w * size[1] / (float) size[0]);
                int x = screenW - w - 10;
                int y = 10;
                int border = 3;
                int alphaByte = alpha(a) << 24;
                // a parchment border like the portfolio's pages, then the picture with the portfolio's photo corners
                g.fill(x - border - 1, y - border - 1, x + w + border + 1, y + h + border + 1, alphaByte | 0x4C3A2A);
                g.fill(x - border, y - border, x + w + border, y + h + border, alphaByte | 0xF1E7D0);
                GuiCompat.tinted(g, alphaByte | 0xFFFFFF, () -> g.blit(picture, x, y, w, h, (float) (0.0F), (float) (0.0F), size[0], size[1], size[0], size[1]));
                int c = 8;
                ResourceLocation tl = PortfolioScreen.ui("photo_corner_tl");
                ResourceLocation tr = PortfolioScreen.ui("photo_corner_tr");
                ResourceLocation bl = PortfolioScreen.ui("photo_corner_bl");
                ResourceLocation br = PortfolioScreen.ui("photo_corner_br");
                int col = alphaByte | 0xFFFFFF;
                GuiCompat.tinted(g, col, () -> g.blit(tl, x - 1, y - 1, c, c, (float) (0.0F), (float) (0.0F), 19, 19, 19, 19));
                GuiCompat.tinted(g, col, () -> g.blit(tr, x + w - c + 1, y - 1, c, c, (float) (0.0F), (float) (0.0F), 19, 19, 19, 19));
                GuiCompat.tinted(g, col, () -> g.blit(bl, x - 1, y + h - c + 1, c, c, (float) (0.0F), (float) (0.0F), 19, 19, 19, 19));
                GuiCompat.tinted(g, col, () -> g.blit(br, x + w - c + 1, y + h - c + 1, c, c, (float) (0.0F), (float) (0.0F), 19, 19, 19, 19));
            }
        }
        if (flashStart >= 0L) {
            long t = now - flashStart;
            if (t > FLASH_MS) {
                flashStart = -1L;
            } else {
                float p = t / (float) FLASH_MS;
                // Bedrock's anim_screenshot_flash: alpha 1 -> 0 with in_expo easing
                float eased = p == 0.0F ? 0.0F : (float) Math.pow(2.0, 10.0 * p - 10.0);
                g.fill(0, 0, screenW, screenH, (alpha(1.0F - eased) << 24) | 0xFFFFFF);
            }
        }
    }

    private PhotoToast() {
    }
}
