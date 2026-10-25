package edu.chemistry.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import edu.chemistry.EduMod;
import edu.chemistry.entity.NpcEntity;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Education's NPC: geometry.npc (a villager-like body with folded arms) wearing one of the NPC skins; it watches nearby players. */
public class NpcRenderer extends EntityRenderer<NpcEntity> {
    private final ModelPart root;
    private final ModelPart head;

    public NpcRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.root = createModel().bakeRoot();
        this.head = this.root.getChild("body").getChild("head");
        this.shadowRadius = 0.5F;
    }

    public static ResourceLocation skinTexture(String skin) {
        return EduMod.id("textures/entity/npc/" + skin + ".png");
    }

    /**
     * geometry.npc from Minecraft Education (npc.geo.json), converted from Bedrock (y up, x mirrored) to Java model
     * space. The arms are folded like a villager's, as the Bedrock NPC shows them (same pivot as Java's villager).
     */
    public static LayerDefinition createModel() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(16, 20).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F)
                .texOffs(0, 38).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 18.0F, 6.0F, new CubeDeformation(0.5F)),
            PartPose.ZERO);
        PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F),
            PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.5F)),
            PartPose.ZERO);
        head.addOrReplaceChild("nose", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-1.0F, -1.0F, -6.0F, 2.0F, 4.0F, 2.0F),
            PartPose.offset(0.0F, -2.0F, 0.0F));
        // Bedrock keeps the geometry's x axis (only y is flipped), so the right arm/leg stays on the -x side
        body.addOrReplaceChild("arms", CubeListBuilder.create()
                .texOffs(40, 38).addBox(-4.0F, 2.0F, -2.0F, 8.0F, 4.0F, 4.0F)
                .texOffs(44, 22).addBox(-8.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F)
                .texOffs(44, 46).addBox(4.0F, -2.0F, -2.0F, 4.0F, 8.0F, 4.0F),
            PartPose.offsetAndRotation(0.0F, 3.0F, -1.0F, -0.75F, 0.0F, 0.0F));
        // leg0 is the right leg, leg1 the left; both use the same texture like Bedrock (no mirroring)
        root.addOrReplaceChild("leg0", CubeListBuilder.create()
                .texOffs(0, 22).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
            PartPose.offset(-2.0F, 12.0F, 0.0F));
        root.addOrReplaceChild("leg1", CubeListBuilder.create()
                .texOffs(0, 22).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
            PartPose.offset(2.0F, 12.0F, 0.0F));
        // The geometry's jacket and pants overlay bones aren't drawn: their texture areas overlap the legs, robe and
        // held items in every Education NPC skin (drawing them puts stray strips on the knees and chest).
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public ResourceLocation getTextureLocation(NpcEntity npc) {
        return skinTexture(npc.getSkin());
    }

    @Override
    public void render(NpcEntity npc, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light) {
        // the body faces the NPC's own direction; the head looks where the NPC is looking (it looks around now and
        // then, head first with the body following, like Education's)
        float bodyYaw = Mth.rotLerp(partialTicks, npc.yRotO, npc.getYRot());
        float head = Float.isNaN(npc.headYaw) ? bodyYaw
            : Mth.rotLerp(partialTicks, Float.isNaN(npc.headYawO) ? npc.headYaw : npc.headYawO, npc.headYaw);
        float headYaw = Mth.clamp(Mth.wrapDegrees(head - bodyYaw), -75.0F, 75.0F);
        float headPitch = Mth.lerp(partialTicks, npc.headPitchO, npc.headPitch);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - bodyYaw));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        this.head.yRot = headYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;
        this.root.render(poseStack, buffers.getBuffer(RenderType.entityCutout(skinTexture(npc.getSkin()))), light, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        super.render(npc, entityYaw, partialTicks, poseStack, buffers, light);
    }
}
