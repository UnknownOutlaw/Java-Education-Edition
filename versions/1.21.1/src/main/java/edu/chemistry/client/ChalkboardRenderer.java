package edu.chemistry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import edu.chemistry.EduMod;
import edu.chemistry.block.ChalkboardBlock;
import edu.chemistry.block.ChalkboardBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a chalkboard from its bottom-left cell: a thin slate panel in a wooden frame (flat against the wall, or standing
 * on wooden legs) with the text written on it in chalk.
 */
public class ChalkboardRenderer implements BlockEntityRenderer<ChalkboardBlockEntity> {
    /** chalk white, drawn at full brightness like Education's */
    private static final int CHALK = 0xFFFFFFFF;
    private static final int LINE_HEIGHT = 10;
    private final Font font;

    public ChalkboardRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.getFont();
    }

    /** the chalkboard is bigger than its block, so don't cull it with that block */
    @Override
    public boolean shouldRenderOffScreen(ChalkboardBlockEntity be) {
        return true;
    }

    /** Splits the text the same way the editor box does: at line breaks, and wherever a line gets too wide. */
    public static List<FormattedCharSequence> wrap(Font font, String text, ChalkboardBlock.Size size) {
        List<FormattedCharSequence> out = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            if (paragraph.isEmpty()) {
                out.add(FormattedCharSequence.EMPTY);
            } else {
                out.addAll(font.split(FormattedText.of(paragraph), size.textWidth));
            }
            if (out.size() >= size.lines) break;
        }
        return out.size() > size.lines ? out.subList(0, size.lines) : out;
    }

    public static ResourceLocation texture(ChalkboardBlock.Size size, boolean wall) {
        return EduMod.id("textures/entity/chalkboard/" + size.name + (wall ? "_wall" : "_standing") + ".png");
    }

    // panel size in pixels
    private static int panelW(ChalkboardBlock.Size size) {
        return size.width * 16;
    }

    /** 4px short of the full height: wall ones stop 4px above the bottom, standing ones stand on 4px legs */
    private static int panelH(ChalkboardBlock.Size size, boolean wall) {
        return size.height * 16 - 4;
    }

    @Override
    public void render(ChalkboardBlockEntity be, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        BlockState block = be.getBlockState();
        if (!block.hasProperty(ChalkboardBlock.FACING)) return;
        ChalkboardBlock.Size size = be.size();
        boolean wall = block.hasProperty(ChalkboardBlock.WALL) && block.getValue(ChalkboardBlock.WALL);
        float angle = !wall && size == ChalkboardBlock.Size.SLATE
            ? RotationSegment.convertToDegrees(block.getValue(ChalkboardBlock.ROTATION))
            : block.getValue(ChalkboardBlock.FACING).toYRot();
        List<FormattedCharSequence> lines = wrap(this.font, be.getText(), size);
        int w = panelW(size);
        int h = panelH(size, wall);
        float bottom = 4;
        float z0 = wall ? 0 : 7.5F;
        float z1 = z0 + 1;

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-angle));
        poseStack.translate(-0.5F, 0.0F, -0.5F);

        int texW = w;
        int texH = 2 * h + 4;
        {
            Box box = new Box(poseStack.last(), buffers.getBuffer(RenderType.entityCutout(texture(size, wall))), light, texW, texH);
            float[] front = {0, 0, w, h};
            float[] back = {0, h, w, 2 * h};
            float[] wood = {0, 2 * h, 4, 2 * h + 4};
            box.draw(0, bottom, z0, w, bottom + h, z1, front, back, wood);
            if (!wall) {
                if (size == ChalkboardBlock.Size.SLATE) {
                    box.draw(7, 0, 7, 9, 4, 9, wood, wood, wood);
                } else {
                    box.draw(2, 0, 7, 4, 4, 9, wood, wood, wood);
                    box.draw(w - 4, 0, 7, w - 2, 4, 9, wood, wood, wood);
                }
            }
        }

        // chalk writing, in the slate area inside the frame
        // inside the 1px wooden frame
        float innerW = w - 2;
        float innerH = h - 2;
        // a full line of writing spans the slate from one side of the frame to the other
        float scale = innerW / size.textWidth;
        if (size.lines * LINE_HEIGHT * scale > innerH) scale = innerH / (size.lines * LINE_HEIGHT);
        float left = 1.0F;
        float top = bottom + h - 1.0F;
        poseStack.translate(left / 16.0F, top / 16.0F, (z1 + 0.05F) / 16.0F);
        poseStack.scale(scale / 16.0F, -scale / 16.0F, scale / 16.0F);
        for (int i = 0; i < lines.size(); i++) {
            this.font.drawInBatch(lines.get(i), 0.0F, i * LINE_HEIGHT, CHALK, false, poseStack.last().pose(), buffers,
                Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
        }
        poseStack.popPose();
    }

    /** Draws boxes in pixel units (1/16 of a block) with UVs in texture pixels. */
    private record Box(PoseStack.Pose pose, VertexConsumer vc, int light, int texW, int texH) {
        void draw(float x0, float y0, float z0, float x1, float y1, float z1, float[] front, float[] back, float[] side) {
            x0 /= 16; y0 /= 16; z0 /= 16; x1 /= 16; y1 /= 16; z1 /= 16;
            this.quad(front, 0, 0, 1, x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1);
            this.quad(back, 0, 0, -1, x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0);
            this.quad(side, 1, 0, 0, x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0);
            this.quad(side, -1, 0, 0, x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1);
            this.quad(side, 0, 1, 0, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0);
            this.quad(side, 0, -1, 0, x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1);
        }

        private void quad(float[] uv, float nx, float ny, float nz, float... p) {
            float u0 = uv[0] / this.texW;
            float v0 = uv[1] / this.texH;
            float u1 = uv[2] / this.texW;
            float v1 = uv[3] / this.texH;
            float[][] uvs = {{u0, v0}, {u0, v1}, {u1, v1}, {u1, v0}};
            for (int i = 0; i < 4; i++) {
                this.vc.addVertex(this.pose, p[i * 3], p[i * 3 + 1], p[i * 3 + 2]).setColor(-1).setUv(uvs[i][0], uvs[i][1])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(this.light).setNormal(this.pose, nx, ny, nz);
            }
        }
    }
}
