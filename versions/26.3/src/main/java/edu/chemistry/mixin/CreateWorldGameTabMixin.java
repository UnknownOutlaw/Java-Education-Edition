package edu.chemistry.mixin;

import edu.chemistry.EduRules;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds an "Education Edition: ON/OFF" button to the Game tab of the Create New World screen, like Bedrock. */
@Mixin(targets = "net.minecraft.client.gui.screens.worldselection.CreateWorldScreen$GameTab")
public abstract class CreateWorldGameTabMixin extends GridLayoutTab {
    private CreateWorldGameTabMixin(Component title) {
        super(title);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void edu$addEducationToggle(CreateWorldScreen screen, CallbackInfo ci) {
        WorldCreationUiState state = screen.getUiState();
        CycleButton<Boolean> button = CycleButton.onOffBuilder(state.getGameRules().get(EduRules.EDUCATION_FEATURES))
            .withTooltip(on -> Tooltip.create(Component.translatable("createWorld.edu.education.info")))
            .create(0, 0, 210, 20, Component.translatable("createWorld.edu.education"),
                (b, on) -> state.getGameRules().set(EduRules.EDUCATION_FEATURES, on, null));
        // below Allow Commands (and the Experiments button that snapshots show)
        int row = SharedConstants.getCurrentVersion().stable() ? 4 : 5;
        this.layout.addChild(button, row, 0, this.layout.newCellSettings());
        state.addListener(s -> button.setValue(s.getGameRules().get(EduRules.EDUCATION_FEATURES)));
    }
}
