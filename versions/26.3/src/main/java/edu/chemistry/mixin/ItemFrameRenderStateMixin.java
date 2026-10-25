package edu.chemistry.mixin;

import edu.chemistry.client.photo.PhotoFrameState;
import edu.chemistry.client.photo.PhotoRender;
import net.minecraft.client.renderer.entity.state.ItemFrameRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/** Lets an item frame's render state carry the photo it shows. */
@Mixin(ItemFrameRenderState.class)
public abstract class ItemFrameRenderStateMixin implements PhotoFrameState {
    @Unique
    private PhotoRender.Picture edu$photo;

    @Override
    public PhotoRender.Picture edu$photo() {
        return this.edu$photo;
    }

    @Override
    public void edu$setPhoto(PhotoRender.Picture photo) {
        this.edu$photo = photo;
    }
}
