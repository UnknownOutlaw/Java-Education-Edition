package edu.chemistry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import edu.chemistry.entity.TripodCameraEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/**
 * The tripod camera: geometry.tripod_camera (a camera box on four splayed legs, the legs at 18 degrees like
 * animation.tripod_camera.neutral) with Education's camera_tripod texture.
 */
public class TripodCameraRenderer extends EntityRenderer<TripodCameraEntity> {
    private static final ResourceLocation TEXTURE = edu.chemistry.EduMod.id("textures/entity/camera_tripod.png");
    private final ModelPart root;
    private final ModelPart body;

    public TripodCameraRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.root = createModel().bakeRoot();
        this.body = this.root.getChild("body");
        this.shadowRadius = 0.4F;
    }

    public static LayerDefinition createModel() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        // Bedrock places the camera 25 px up so the legs stand on the ground (Java y = 24 is the ground)
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -4.0F, -5.0F, 8.0F, 8.0F, 10.0F),
            PartPose.offset(0.0F, -1.0F, 0.0F));
        float splay = 18.0F * Mth.DEG_TO_RAD;
        // the legs hang from the camera's pivot (body at y -1, legs 3 px below it) but stay put when the camera tilts
        CubeListBuilder leg = CubeListBuilder.create().texOffs(36, 0).addBox(-0.5F, 1.0F, -0.5F, 1.0F, 22.0F, 1.0F);
        root.addOrReplaceChild("leg0", leg, PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, splay, 0.0F, 0.0F));
        root.addOrReplaceChild("leg1", leg, PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, -splay, 0.0F, 0.0F));
        root.addOrReplaceChild("leg2", leg, PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, 0.0F, 0.0F, splay));
        root.addOrReplaceChild("leg3", leg, PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, 0.0F, 0.0F, -splay));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** not while you're looking through it (taking a picture from the tripod) */
    @Override
    public boolean shouldRender(TripodCameraEntity camera, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        if (net.minecraft.client.Minecraft.getInstance().getCameraEntity() == camera) return false;
        return super.shouldRender(camera, frustum, x, y, z);
    }

    @Override
    public ResourceLocation getTextureLocation(TripodCameraEntity camera) {
        return TEXTURE;
    }

    @Override
    public void render(TripodCameraEntity camera, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        float yaw = Mth.rotLerp(partialTicks, camera.yRotO, camera.getYRot());
        float pitch = Mth.lerp(partialTicks, camera.xRotO, camera.getXRot());
        // flashes white like lit TNT while counting down
        int timer = camera.getTimer();
        boolean flash = timer > 0 && timer / 5 % 2 == 0;
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        // the camera tilts toward what it looks at; the legs stay on the ground
        this.body.xRot = pitch * Mth.DEG_TO_RAD;
        int overlay = flash ? OverlayTexture.pack(OverlayTexture.u(1.0F), 10) : OverlayTexture.NO_OVERLAY;
        this.root.render(poseStack, buffers.getBuffer(RenderType.entityCutout(TEXTURE)), light, overlay);
        this.body.xRot = 0.0F;
        poseStack.popPose();
        super.render(camera, entityYaw, partialTicks, poseStack, buffers, light);
    }
}
