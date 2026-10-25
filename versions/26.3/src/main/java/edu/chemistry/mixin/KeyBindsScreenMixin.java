package edu.chemistry.mixin;

import edu.chemistry.client.ControlTips;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LayoutElement;
import net.minecraft.client.gui.layouts.LayoutSettings;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * The Control Tips on/off toggle, in the Key Binds menu (Options > Controls > Key Binds): a full-width button above
 * Reset Keys and Done, with the footer made taller to fit it.
 */
@Mixin(KeyBindsScreen.class)
public abstract class KeyBindsScreenMixin {
    /** the toggle's height plus the gap above the bottom buttons */
    private static final int EDU_EXTRA_FOOTER = 24;

    @Redirect(method = "addFooter", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/gui/layouts/HeaderAndFooterLayout;addToFooter(Lnet/minecraft/client/gui/layouts/LayoutElement;)Lnet/minecraft/client/gui/layouts/LayoutElement;"))
    private LayoutElement edu$controlTipsToggle(HeaderAndFooterLayout layout, LayoutElement buttons) {
        ControlTips.ensureLoaded();
        LinearLayout column = LinearLayout.vertical().spacing(4);
        // as wide as the Reset Keys and Done buttons together
        column.addChild(ControlTips.OPTION.createButton(Minecraft.getInstance().options, 0, 0, 308), LayoutSettings::alignHorizontallyCenter);
        column.addChild(buttons, LayoutSettings::alignHorizontallyCenter);
        layout.addToFooter(column);
        layout.setFooterHeight(HeaderAndFooterLayout.DEFAULT_HEADER_AND_FOOTER_HEIGHT + EDU_EXTRA_FOOTER);
        // the menu goes on to put Reset Keys and Done into the row it gets back
        return buttons;
    }
}
