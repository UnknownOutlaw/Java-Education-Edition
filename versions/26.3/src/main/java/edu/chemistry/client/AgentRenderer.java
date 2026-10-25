package edu.chemistry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import edu.chemistry.EduMod;
import edu.chemistry.agent.AgentEntity;
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
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Education's Agent: geometry.agent with its own texture, animated like Education's (agent.animation.json): it bobs
 * gently and sways its long arms while idle, marches when it steps, swings its arm when it works, shrugs when it can't
 * do something, and turns its head to watch players nearby.
 */
public class AgentRenderer extends EntityRenderer<AgentEntity, AgentRenderer.State> {
    private static final Identifier TEXTURE = EduMod.id("textures/entity/agent.png");
    /** the arm swing that goes with an action, in ticks */
    private static final float SWING_TICKS = 12.0F;
    /** animation.agent.shrug is 1.5 s long */
    private static final float SHRUG_SECONDS = 1.5F;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public AgentRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.root = createModel().bakeRoot();
        this.body = this.root.getChild("body");
        this.head = this.body.getChild("head");
        this.leftArm = this.body.getChild("leftArm");
        this.rightArm = this.body.getChild("rightArm");
        this.leftLeg = this.body.getChild("leftLeg");
        this.rightLeg = this.body.getChild("rightLeg");
        this.shadowRadius = 0.3F;
    }

    /**
     * geometry.agent from Minecraft Education (mobs.json), converted from Bedrock (y up) to Java model space (y down,
     * the ground at 24). Like the other Education models here, the x axis is kept as it is.
     */
    public static LayerDefinition createModel() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 12).addBox(-4.0F, 15.0F, -2.0F, 8.0F, 4.0F, 4.0F)
                .texOffs(0, 20).addBox(-3.0F, 19.0F, -2.0F, 6.0F, 2.0F, 4.0F),
            PartPose.ZERO);
        body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-3.0F, -7.0F, -2.2F, 6.0F, 7.0F, 5.0F)
                .texOffs(17, 1).addBox(-1.0F, -3.0F, -3.2F, 2.0F, 3.0F, 1.0F),
            PartPose.offset(0.0F, 15.0F, -0.8F));
        body.addOrReplaceChild("leftArm", CubeListBuilder.create()
                .texOffs(24, 11).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 9.0F, 2.0F),
            PartPose.offset(5.0F, 15.0F, 0.0F));
        body.addOrReplaceChild("rightArm", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 9.0F, 2.0F),
            PartPose.offset(-5.0F, 15.0F, 0.0F));
        body.addOrReplaceChild("leftLeg", CubeListBuilder.create()
                .texOffs(8, 26).addBox(-0.9F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
            PartPose.offset(1.9F, 21.0F, 0.0F));
        body.addOrReplaceChild("rightLeg", CubeListBuilder.create()
                .texOffs(0, 26).addBox(-1.1F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F),
            PartPose.offset(-1.9F, 21.0F, 0.0F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    public static class State extends EntityRenderState {
        public float bodyYaw;
        public float headYaw;
        public float headPitch;
        /** ticks since it appeared */
        public float age;
        public float walk;
        public float speed;
        public boolean hovering;
        public int animKind;
        /** ticks since the last swing or shrug started */
        public float animTime = 1000.0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(AgentEntity agent, State state, float partialTicks) {
        super.extractRenderState(agent, state, partialTicks);
        state.bodyYaw = agent.getYRot(partialTicks);
        state.headYaw = Mth.rotLerp(partialTicks, agent.headYawO, agent.headYaw);
        state.headPitch = Mth.lerp(partialTicks, agent.headPitchO, agent.headPitch);
        state.age = agent.tickCount + partialTicks;
        state.walk = Mth.lerp(partialTicks, agent.walkDistO, agent.walkDist);
        state.speed = Mth.lerp(partialTicks, agent.moveSpeedO, agent.moveSpeed);
        state.hovering = agent.hovering;
        state.animKind = agent.animKind;
        state.animTime = state.age - agent.animStart;
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, 180.0F - state.bodyYaw);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        this.root.getAllParts().forEach(ModelPart::resetPose);
        this.pose(state);
        collector.submitModelPart(this.root, poseStack, RenderTypes.entityCutout(TEXTURE), state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    // ---------------------------------------------------------------- animation (degrees, as in Bedrock)

    private static float sinDeg(float degrees) {
        return Mth.sin(degrees * Mth.DEG_TO_RAD);
    }

    private static float cosDeg(float degrees) {
        return Mth.cos(degrees * Mth.DEG_TO_RAD);
    }

    /** Bedrock rotations (degrees) to a Java part: x as is, y turned the other way, z as is */
    private static void rotate(ModelPart part, float x, float y, float z) {
        part.xRot = x * Mth.DEG_TO_RAD;
        part.yRot = -y * Mth.DEG_TO_RAD;
        part.zRot = z * Mth.DEG_TO_RAD;
    }

    private void pose(State state) {
        float t = state.age / 20.0F;
        // the entity's pre_animation script
        float armZ = (cosDeg(t * 114.6F) + 1.0F) * 1.15F;
        float armXFactor = sinDeg(t * 80.0F) * 4.6F;
        float legXFactor = sinDeg(t * 46.0F) * 1.15F;
        float hovering = state.hovering ? 1.0F : 0.0F;
        float easeIn = Math.min(1.0F, state.age / 20.0F);

        // animation.agent.move (it plays all the time; standing still it's the idle sway)
        float stride = (Math.abs(Mth.positiveModulo(state.walk, 13.0F) - 6.5F) - 3.25F) / 3.25F;
        float speed = state.speed;
        this.body.y -= easeIn * (sinDeg(t * 114.6F) + 1.0F) * 1.12F;
        rotate(this.leftArm, (-0.2F + 1.5F * stride) * speed * 57.3F - armXFactor, 0.0F, -armZ);
        rotate(this.rightArm, (-0.2F - 1.5F * stride) * speed * 57.3F + armXFactor, 0.0F, armZ);
        rotate(this.leftLeg, -1.5F * stride * speed * 57.3F + hovering * legXFactor, 0.0F, 0.0F);
        rotate(this.rightLeg, 1.5F * stride * speed * 57.3F - hovering * legXFactor, 0.0F, 0.0F);

        // animation.common.look_at_target
        this.head.yRot = state.headYaw * Mth.DEG_TO_RAD;
        this.head.xRot = state.headPitch * Mth.DEG_TO_RAD;

        if (state.animKind == AgentEntity.ANIM_SWING && state.animTime < SWING_TICKS) {
            // animation.agent.swing_arms, once: the right arm chops down from over its head
            float p = Math.max(0.0F, state.animTime) / SWING_TICKS;
            float weight = Math.min(1.0F, Math.min(p * 4.0F, (1.0F - p) * 4.0F));
            float swing = (-2.0F + 1.5F * (Math.abs(p - 0.5F) * 4.0F - 1.0F)) * 57.3F;
            float x = Mth.lerp(weight, this.rightArm.xRot * Mth.RAD_TO_DEG, swing);
            this.rightArm.xRot = x * Mth.DEG_TO_RAD;
        } else if (state.animKind == AgentEntity.ANIM_SHRUG && state.animTime < SHRUG_SECONDS * 20.0F) {
            this.shrug(Math.max(0.0F, state.animTime) / 20.0F);
        }
    }

    private static final float[] HEAD_TIMES = {0.3333F, 0.5833F, 0.8333F, 1.0417F, 1.2917F, 1.5F};
    private static final float[][] HEAD_ROT = {{0, 0, 0}, {0, 0, 0}, {0, 72.5F, 0}, {0, 0, 0}, {0, -62.5F, 0}, {0, 0, 0}};
    private static final float[] ARM_TIMES = {0.0F, 0.25F, 0.625F, 0.75F, 0.9583F, 1.1667F};
    private static final float[][] LEFT_ARM_ROT = {{0, 0, 0}, {-64.03195F, -33.38529F, 26.32435F}, {0, 0, 0}, {0, 0, 0}, {0, 0, -10}, {0, 0, 0}};
    private static final float[][] RIGHT_ARM_ROT = {{0, 0, 0}, {-58.25703F, 25.73828F, -15.06695F}, {0, 0, 0}, {0, 0, 0}, {0, 0, 10}, {0, 0, 0}};
    private static final float[] ARM_POS_TIMES = {0.75F, 0.9583F, 1.1667F};
    private static final float[][] ARM_POS = {{0, 0, 0}, {0, 1, 0}, {0, 0, 0}};

    /** animation.agent.shrug: arms up and out, a lift of the shoulders and a shake of the head */
    private void shrug(float seconds) {
        float[] head = keyframe(HEAD_TIMES, HEAD_ROT, seconds);
        this.head.yRot -= head[1] * Mth.DEG_TO_RAD;
        float[] left = keyframe(ARM_TIMES, LEFT_ARM_ROT, seconds);
        float[] right = keyframe(ARM_TIMES, RIGHT_ARM_ROT, seconds);
        rotate(this.leftArm, left[0], left[1], left[2]);
        rotate(this.rightArm, right[0], right[1], right[2]);
        float lift = keyframe(ARM_POS_TIMES, ARM_POS, seconds)[1];
        this.leftArm.y -= lift;
        this.rightArm.y -= lift;
    }

    /** linear keyframes, held before the first and after the last (Bedrock's default) */
    private static float[] keyframe(float[] times, float[][] values, float t) {
        if (t <= times[0]) return values[0];
        for (int i = 1; i < times.length; i++) {
            if (t <= times[i]) {
                float f = (t - times[i - 1]) / (times[i] - times[i - 1]);
                float[] a = values[i - 1];
                float[] b = values[i];
                return new float[] {Mth.lerp(f, a[0], b[0]), Mth.lerp(f, a[1], b[1]), Mth.lerp(f, a[2], b[2])};
            }
        }
        return values[values.length - 1];
    }
}
