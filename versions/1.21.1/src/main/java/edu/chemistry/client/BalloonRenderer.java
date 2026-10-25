package edu.chemistry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import edu.chemistry.EduMod;
import edu.chemistry.entity.BalloonEntity;
import java.util.Optional;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * Bedrock's balloon: geometry.balloon (balloon.geo.json) on its own 22x22 greyscale texture, tinted with the balloon's
 * dye colour, plus the lead-rope string down to whatever it is tied to.
 */
public class BalloonRenderer extends EntityRenderer<BalloonEntity> {
    private static final ResourceLocation TEXTURE = EduMod.id("textures/entity/balloon.png");
    private final ModelPart model;
    /** length of the bit of lead hanging under a balloon that isn't tied to anything */
    private static final double DANGLE_LENGTH = 0.6;
    /** same height as vanilla's lead knot on a fence (LeashFenceKnotEntity.OFFSET_Y) */
    private static final double KNOT_Y = 0.375;
    private static final ResourceLocation KNOT_TEXTURE = ResourceLocation.withDefaultNamespace("textures/entity/lead_knot.png");
    private final ModelPart knotModel;

    public BalloonRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = createModel().bakeRoot();
        this.knotModel = context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.LEASH_KNOT);
        this.shadowRadius = 0.2F;
        this.shadowStrength = 0.4F;
    }

    /**
     * geometry.balloon converted from Bedrock (y up) to Java model space (y down, x mirrored). Bones keep Bedrock's
     * pivots; box UVs are the same layout in both editions.
     */
    public static LayerDefinition createModel() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // "body": front and back panels, 6x7x1 at uv (0,7)
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 7).addBox(-3.0008F, -8.9992F, -4.0F, 6.0F, 7.0F, 1.0F)
                .texOffs(0, 7).addBox(-3.0F, -9.0F, 3.0008F, 6.0F, 7.0F, 1.0F),
            PartPose.ZERO);
        // "head": the same two panels turned 90 degrees to make the left and right sides
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 7).addBox(-2.99996F, -4.0F, -4.00004F, 6.0F, 7.0F, 1.0F)
                .texOffs(0, 7).addBox(-3.0F, -4.0008F, 2.9992F, 6.0F, 7.0F, 1.0F),
            PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, 0.0F, (float) (-Math.PI / 2), 0.0F));
        // "rightArm": bottom cap (uv 0,15), top cap (uv 0,0) and the 2x2 knot (uv 14,0), turned to lie flat
        root.addOrReplaceChild("rightArm", CubeListBuilder.create()
                .texOffs(0, 15).addBox(-3.0F, -3.0F, -4.0F, 6.0F, 6.0F, 1.0F)
                .texOffs(0, 0).addBox(-3.0F, -2.99919F, 4.0F, 6.0F, 6.0F, 1.0F)
                .texOffs(14, 0).addBox(-1.0008F, -0.99997F, -4.99922F, 2.0F, 2.0F, 1.0F),
            PartPose.offsetAndRotation(0.0F, -5.0F, 0.0F, (float) (Math.PI / 2), 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 22, 22);
    }

    @Override
    public ResourceLocation getTextureLocation(BalloonEntity balloon) {
        return TEXTURE;
    }

    /** Covers the balloon and its whole lead, so the lead stays visible even when the balloon itself is off screen. */
    @Override
    public boolean shouldRender(BalloonEntity entity, Frustum frustum, double camX, double camY, double camZ) {
        if (!entity.shouldRender(camX, camY, camZ)) return false;
        if (entity.noCulling) return true;
        AABB box = entity.getBoundingBoxForCulling().inflate(0.5, 0.5, 0.5).expandTowards(0.0, -DANGLE_LENGTH - 0.5, 0.0);
        Entity holder = entity.getLeashHolder();
        if (holder != null) box = box.minmax(holder.getBoundingBoxForCulling());
        Optional<BlockPos> knot = entity.getKnot();
        if (knot.isPresent()) box = box.minmax(new AABB(knot.get()));
        return frustum.isVisible(box);
    }

    @Override
    public void render(BalloonEntity entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        int color = entity.getColor().getTextureDiffuseColor();
        // each balloon has its own phase and pace, so a bunch of them don't sway in step
        float t = (entity.tickCount + partialTicks) * entity.swayRate();
        float sway = (float) Math.sin(t * 0.1F + entity.swayPhase()) * 3.0F;

        poseStack.pushPose();
        // gentle sway around the knot, which sits at the entity's position like in Bedrock
        poseStack.mulPose(Axis.ZP.rotationDegrees(sway));
        poseStack.mulPose(Axis.XP.rotationDegrees(sway * 0.6F));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        this.model.render(poseStack, buffers.getBuffer(RenderType.entityCutout(TEXTURE)), light, OverlayTexture.NO_OVERLAY, color);
        poseStack.popPose();

        // the lead, always pulled tight by the balloon: to the mob or fence knot it's tied to, to a wall, or dangling
        Vec3 from = entity.getPosition(partialTicks);
        Vec3 end;
        Entity holder = entity.getLeashHolder();
        Optional<BlockPos> wall = entity.getKnot();
        if (holder != null) {
            end = holder.getRopeHoldPosition(partialTicks);
        } else if (wall.isPresent()) {
            // walls can't take vanilla's lead knot, so the balloon draws one there itself
            BlockPos p = wall.get();
            Vec3 knotPos = new Vec3(p.getX() + 0.5, p.getY() + KNOT_Y, p.getZ() + 0.5);
            Vec3 offset = knotPos.subtract(from);
            poseStack.pushPose();
            poseStack.translate(offset.x, offset.y, offset.z);
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            this.knotModel.render(poseStack, buffers.getBuffer(RenderType.entityCutout(KNOT_TEXTURE)), light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
            end = knotPos.add(0.0, 0.2, 0.0);
        } else {
            // untied: a short piece of lead dangles from the knot and trails a little behind as the balloon drifts
            Vec3 motion = entity.getDeltaMovement();
            end = from.add(
                Math.sin(t * 0.1F + entity.swayPhase()) * 0.06 - motion.x * 1.5,
                -DANGLE_LENGTH,
                Math.cos(t * 0.13F + entity.swayPhase()) * 0.06 - motion.z * 1.5);
        }
        drawLead(entity.level(), poseStack, buffers, from, end);

        if (this.shouldShowName(entity)) this.renderNameTag(entity, entity.getDisplayName(), poseStack, buffers, light, partialTicks);
    }

    /**
     * A straight lead from {@code from} (where the balloon is drawn) to {@code to}, built like vanilla's: two ribbons
     * crossed in the plane across its horizontal direction, in alternating shades. A lead that hangs (nearly) straight
     * up and down has no horizontal direction, so it gets one ribbon east-west and one north-south instead.
     */
    private static void drawLead(Level level, PoseStack poseStack, MultiBufferSource buffers, Vec3 from, Vec3 to) {
        float dx = (float) (to.x - from.x);
        float dy = (float) (to.y - from.y);
        float dz = (float) (to.z - from.z);
        BlockPos startPos = BlockPos.containing(from);
        BlockPos endPos = BlockPos.containing(to);
        int[] light = {level.getBrightness(LightLayer.BLOCK, startPos), level.getBrightness(LightLayer.BLOCK, endPos),
            level.getBrightness(LightLayer.SKY, startPos), level.getBrightness(LightLayer.SKY, endPos)};
        VertexConsumer vc = buffers.getBuffer(RenderType.leash());
        Matrix4f pose = poseStack.last().pose();
        float half = 0.025F / 2.0F;
        float horizontal = dx * dx + dz * dz;
        if (horizontal >= 0.0001F) {
            float scale = (float) (half / Math.sqrt(horizontal));
            ribbons(vc, pose, dx, dy, dz, dz * scale, dx * scale, light);
        } else {
            ribbons(vc, pose, dx, dy, dz, half, 0.0F, light);
            ribbons(vc, pose, dx, dy, dz, 0.0F, half, light);
        }
    }

    private static void ribbons(VertexConsumer vc, Matrix4f pose, float dx, float dy, float dz, float sx, float sz, int[] light) {
        for (int i = 0; i <= 24; i++) leadPair(vc, pose, dx, dy, dz, sx, sz, 0.025F, 0.025F, i, false, light);
        for (int i = 24; i >= 0; i--) leadPair(vc, pose, dx, dy, dz, sx, sz, 0.025F, 0.0F, i, true, light);
    }

    private static void leadPair(VertexConsumer vc, Matrix4f pose, float dx, float dy, float dz, float sx, float sz,
                                 float thickness, float lift, int index, boolean back, int[] light) {
        float f = index / 24.0F;
        int packed = LightTexture.pack((int) Mth.lerp(f, light[0], light[1]), (int) Mth.lerp(f, light[2], light[3]));
        float shade = index % 2 == (back ? 1 : 0) ? 0.7F : 1.0F;
        float r = 0.5F * shade;
        float g = 0.4F * shade;
        float b = 0.3F * shade;
        float x = dx * f;
        float y = dy * f;
        float z = dz * f;
        vc.addVertex(pose, x - sx, y + lift, z + sz).setColor(r, g, b, 1.0F).setLight(packed);
        vc.addVertex(pose, x + sx, y + thickness - lift, z - sz).setColor(r, g, b, 1.0F).setLight(packed);
    }
}
