package edu.chemistry.client.photo;

import com.google.common.collect.MapMaker;
import com.mojang.blaze3d.vertex.PoseStack;
import edu.chemistry.EduRules;
import edu.chemistry.item.PhotoItem;
import edu.chemistry.photo.Photos;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * Photos shown the way maps are: held in first person (in both hands, or one hand beside the other item) and in item
 * frames, where the picture covers the frame's face. The picture sits on the map's paper, as large as it fits with its
 * own shape (a wide picture leaves paper above and below it). Sizes are in map units: 128 is the width of a map.
 */
public final class PhotoRender {
    /** a photo's picture, loaded as a texture */
    public record Picture(Identifier texture, int width, int height) {
    }

    private static final RenderType PAPER = RenderTypes.text(Identifier.withDefaultNamespace("textures/map/map_background.png"));
    /** the map paper's border, as a share of the whole texture (7 of its 142 units) */
    private static final float PAPER_BORDER = 7.0F / 142.0F;
    /**
     * Pictures by the item's custom data. That data is replaced (not changed) whenever a photo changes, and is shared
     * by copies of the stack, so looking it up by identity is cheap every frame and never shows a stale picture.
     */
    private static final Map<CustomData, Optional<Picture>> CACHE = new MapMaker().weakKeys().makeMap();

    private PhotoRender() {
    }

    /** the picture to show for this stack, or null when it isn't a photo with a picture (or Education Edition is off) */
    public static @Nullable Picture of(ItemStack stack) {
        if (!EduRules.clientEnabled || !(stack.getItem() instanceof PhotoItem)) return null;
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        Optional<Picture> picture = CACHE.get(data);
        if (picture == null) {
            picture = Optional.ofNullable(load(stack));
            CACHE.put(data, picture);
        }
        return picture.orElse(null);
    }

    private static @Nullable Picture load(ItemStack stack) {
        byte[] png = Photos.image(stack).orElse(null);
        if (png == null) return null;
        String id = Photos.id(stack);
        String key = "item/" + (id.isBlank() ? Integer.toHexString(Arrays.hashCode(png)) : id);
        Identifier texture = PortfolioStore.register(key, png);
        if (texture == null) return null;
        int[] size = PortfolioStore.size(texture);
        return new Picture(texture, size[0], size[1]);
    }

    /** Held in first person: the same placement as a map, then the paper with its border and the picture on it. */
    public static void inHand(PoseStack pose, SubmitNodeCollector out, int light, Picture picture) {
        pose.mulPose(new Matrix4f().rotationY((float) Math.PI));
        pose.mulPose(new Matrix4f().rotationZ((float) Math.PI));
        pose.scale(0.38F, 0.38F, 0.38F);
        pose.translate(-0.5F, -0.5F, 0.0F);
        pose.scale(1.0F / 128.0F, 1.0F / 128.0F, 1.0F / 128.0F);
        quad(pose, out, PAPER, -7.0F, -7.0F, 142.0F, 142.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, light);
        picture(pose, out, picture, -0.01F, light);
    }

    /**
     * In an item frame, from where the frame puts its item: turned like a map (quarter turns only) and covering the
     * frame's whole face, on the paper without its border.
     */
    public static void inFrame(PoseStack pose, SubmitNodeCollector out, int light, Picture picture, int rotation) {
        pose.mulPose(new Matrix4f().rotationZ((float) Math.toRadians(rotation % 4 * 90.0F)));
        pose.mulPose(new Matrix4f().rotationZ((float) Math.PI));
        pose.scale(1.0F / 128.0F, 1.0F / 128.0F, 1.0F / 128.0F);
        pose.translate(-64.0F, -64.0F, 0.0F);
        pose.translate(0.0F, 0.0F, -1.0F);
        quad(pose, out, PAPER, 0.0F, 0.0F, 128.0F, 128.0F, PAPER_BORDER, PAPER_BORDER, 1.0F - PAPER_BORDER, 1.0F - PAPER_BORDER, -0.01F, light);
        picture(pose, out, picture, -0.02F, light);
    }

    /** the picture, centred in the 128 x 128 map area and as big as fits */
    private static void picture(PoseStack pose, SubmitNodeCollector out, Picture picture, float z, int light) {
        float scale = Math.min(128.0F / picture.width(), 128.0F / picture.height());
        float w = picture.width() * scale;
        float h = picture.height() * scale;
        quad(pose, out, RenderTypes.text(picture.texture()), (128.0F - w) / 2.0F, (128.0F - h) / 2.0F, w, h, 0.0F, 0.0F, 1.0F, 1.0F, z, light);
    }

    private static void quad(PoseStack pose, SubmitNodeCollector out, RenderType type, float x, float y, float w, float h,
                             float u0, float v0, float u1, float v1, float z, int light) {
        out.submitCustomGeometry(pose, type, (p, buffer) -> {
            buffer.addVertex(p, x, y + h, z).setColor(-1).setUv(u0, v1).setLight(light);
            buffer.addVertex(p, x + w, y + h, z).setColor(-1).setUv(u1, v1).setLight(light);
            buffer.addVertex(p, x + w, y, z).setColor(-1).setUv(u1, v0).setLight(light);
            buffer.addVertex(p, x, y, z).setColor(-1).setUv(u0, v0).setLight(light);
        });
    }
}
