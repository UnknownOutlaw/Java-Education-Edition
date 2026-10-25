package edu.chemistry.mixin;

import edu.chemistry.item.GlowStickItem;
import edu.chemistry.item.LitState;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * A lit glow stick is drawn at full brightness whatever the light around it, like Bedrock's. The newer builds say so in
 * the lit model (light_emission); 1.21.1's models can't, so the light is raised here.
 */
@Mixin(ItemRenderer.class)
public abstract class ItemRendererGlowMixin {
    @ModifyVariable(method = "render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V",
        at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int edu$glow(int light, ItemStack stack) {
        if (stack.getItem() instanceof GlowStickItem && LitState.isLit(stack)) return LightTexture.pack(15, LightTexture.sky(light));
        return light;
    }
}
