package edu.chemistry.mixin;

import java.util.List;
import net.minecraft.client.gui.components.SubtitleOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** how many closed captions are showing, so the control tips can sit above them */
@Mixin(SubtitleOverlay.class)
public interface SubtitleOverlayAccessor {
    @Accessor("subtitles")
    List<?> edu$subtitles();
}
