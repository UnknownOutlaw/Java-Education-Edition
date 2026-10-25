package edu.chemistry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import edu.chemistry.EduMod;
import edu.chemistry.entity.BalloonEntity;
import java.util.ArrayList;
import java.util.Optional;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Bedrock's balloon: geometry.balloon (balloon.geo.json) on its own 22x22 greyscale texture, tinted with the balloon's
 * dye colour, plus the lead-rope string down to whatever it is tied to.
 */
public class BalloonRenderer extends EntityRenderer<BalloonEntity, BalloonRenderer.State> {
    private static final Identifier TEXTURE = EduMod.id("textures/entity/balloon.png");
    private final ModelPart model;
    /** length of the bit of lead hanging under a balloon that isn't tied to anything */
    private static final double DANGLE_LENGTH = 0.6;
    /** same height as vanilla's lead knot on a fence (LeashFenceKnotEntity.OFFSET_Y) */
    private static final double KNOT_Y = 0.375;
    private static final Identifier KNOT_TEXTURE = Identifier.withDefaultNamespace("textures/entity/lead_knot/lead_knot.png");
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

    public static class State extends EntityRenderState {
        public int color = -1;
        /** where the balloon's own knot is on a wall, relative to the balloon (null otherwise) */
        public @org.jspecify.annotations.Nullable Vec3 knotOffset;
        public float sway;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    /** Covers the balloon and its whole lead, so the lead stays visible even when the balloon itself is off screen. */
    @Override
    protected AABB getBoundingBoxForCulling(BalloonEntity entity, float partialTicks) {
        AABB box = super.getBoundingBoxForCulling(entity, partialTicks).inflate(0.5, 0.5, 0.5).expandTowards(0.0, -DANGLE_LENGTH - 0.5, 0.0);
        Entity attached = entity.getAttachedEntity();
        if (attached != null) {
            box = box.minmax(attached.getBoundingBox());
        }
        Optional<BlockPos> knot = entity.getKnot();
        if (knot.isPresent()) {
            box = box.minmax(new AABB(knot.get()));
        }
        return box;
    }

    @Override
    public void extractRenderState(BalloonEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.color = entity.getColor().getTextureDiffuseColor();
        // each balloon has its own phase and pace, so a bunch of them don't sway in step
        float t = (entity.tickCount + partialTicks) * entity.swayRate();
        state.sway = (float) Math.sin(t * 0.1F + entity.swayPhase()) * 3.0F;

        state.knotOffset = null;
        Vec3 from = entity.getPosition(partialTicks);
        if (state.leashStates != null && !state.leashStates.isEmpty()) {
            // tied to a mob or to a lead knot on a fence: vanilla has already set up a real lead from the balloon's
            // knot to the mob's rope point or the fence knot. A balloon pulls it tight.
            for (EntityRenderState.LeashState leash : state.leashStates) leash.slack = false;
        } else {
            Vec3 end;
            Optional<BlockPos> wall = entity.getKnot();
            if (wall.isPresent()) {
                // walls can't take vanilla's lead knot, so the balloon draws one there itself
                BlockPos p = wall.get();
                Vec3 knotPos = new Vec3(p.getX() + 0.5, p.getY() + KNOT_Y, p.getZ() + 0.5);
                state.knotOffset = knotPos.subtract(from);
                end = knotPos.add(0.0, 0.2, 0.0);
            } else {
                // untied: a short piece of lead dangles from the knot and trails a little behind as the balloon drifts
                Vec3 motion = entity.getDeltaMovement();
                end = from.add(
                    Math.sin(t * 0.1F + entity.swayPhase()) * 0.06 - motion.x * 1.5,
                    -DANGLE_LENGTH,
                    Math.cos(t * 0.13F + entity.swayPhase()) * 0.06 - motion.z * 1.5);
            }
            EntityRenderState.LeashState leash = new EntityRenderState.LeashState();
            leash.offset = Vec3.ZERO;
            leash.start = from;
            leash.end = end;
            Level level = entity.level();
            BlockPos startPos = BlockPos.containing(from);
            BlockPos endPos = BlockPos.containing(end);
            leash.startBlockLight = level.getBrightness(LightLayer.BLOCK, startPos);
            leash.endBlockLight = level.getBrightness(LightLayer.BLOCK, endPos);
            leash.startSkyLight = level.getBrightness(LightLayer.SKY, startPos);
            leash.endSkyLight = level.getBrightness(LightLayer.SKY, endPos);
            leash.slack = false;
            state.leashStates = new ArrayList<>(2);
            state.leashStates.add(leash);
        }
        crossWhenUpright(state.leashStates);
    }

    /**
     * Vanilla's lead is two ribbons crossed in the plane across its horizontal direction, so a lead that hangs (nearly)
     * straight up and down has no proper direction and shows as one flat ribbon - or vanishes. For those, lay one lead
     * east-west and a second one north-south, so it looks like a lead from every side.
     */
    private static void crossWhenUpright(java.util.List<EntityRenderState.LeashState> leads) {
        if (leads.size() != 1) return;
        EntityRenderState.LeashState a = leads.getFirst();
        double hx = a.end.x - a.start.x;
        double hz = a.end.z - a.start.z;
        if (hx * hx + hz * hz >= 0.01) return;
        EntityRenderState.LeashState b = new EntityRenderState.LeashState();
        b.offset = a.offset;
        b.start = a.start;
        b.startBlockLight = a.startBlockLight;
        b.endBlockLight = a.endBlockLight;
        b.startSkyLight = a.startSkyLight;
        b.endSkyLight = a.endSkyLight;
        b.slack = a.slack;
        a.end = new Vec3(a.start.x + 0.01, a.end.y, a.start.z);
        b.end = new Vec3(a.start.x, a.end.y, a.start.z + 0.01);
        leads.add(b);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        // gentle sway around the knot, which sits at the entity's position like in Bedrock
        poseStack.rotateDegrees(Axis.ZP, state.sway);
        poseStack.rotateDegrees(Axis.XP, state.sway * 0.6F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        collector.submitModelPart(this.model, poseStack, RenderTypes.entityCutout(TEXTURE), state.lightCoords, OverlayTexture.NO_OVERLAY,
            null, state.color);
        poseStack.popPose();
        if (state.knotOffset != null) {
            poseStack.pushPose();
            poseStack.translate(state.knotOffset.x, state.knotOffset.y, state.knotOffset.z);
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            collector.submitModelPart(this.knotModel, poseStack, RenderTypes.entityCutout(KNOT_TEXTURE), state.lightCoords,
                OverlayTexture.NO_OVERLAY, null);
            poseStack.popPose();
        }
        super.submit(state, poseStack, collector, camera);
    }
}
