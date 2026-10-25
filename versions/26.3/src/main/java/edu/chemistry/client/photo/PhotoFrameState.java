package edu.chemistry.client.photo;

import org.jspecify.annotations.Nullable;

/** The photo an item frame is showing, kept on its render state (added by ItemFrameRenderStateMixin). */
public interface PhotoFrameState {
    PhotoRender.@Nullable Picture edu$photo();

    void edu$setPhoto(PhotoRender.@Nullable Picture photo);
}
