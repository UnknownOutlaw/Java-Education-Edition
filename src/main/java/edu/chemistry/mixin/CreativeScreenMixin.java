package edu.chemistry.mixin;

import edu.chemistry.client.CreativeCategories;
import edu.chemistry.registry.EduTabs;
import java.util.Collection;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds the Chemistry category side tabs to the creative inventory. */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeScreenMixin extends AbstractContainerScreen<CreativeModeInventoryScreen.ItemPickerMenu> {
    @Shadow
    private static CreativeModeTab selectedTab;

    @Shadow
    private float scrollOffs;

    protected CreativeScreenMixin(CreativeModeInventoryScreen.ItemPickerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    private void edu$applyCategory() {
        CreativeCategories.fill(this.menu.items);
        this.scrollOffs = 0.0F;
        this.menu.scrollTo(0.0F);
    }

    @Inject(method = "selectTab", at = @At("RETURN"))
    private void edu$afterSelectTab(CreativeModeTab tab, CallbackInfo ci) {
        if (tab == EduTabs.CHEMISTRY) this.edu$applyCategory();
    }

    @Inject(method = "refreshCurrentTabContents", at = @At("RETURN"))
    private void edu$afterRefresh(Collection<ItemStack> items, CallbackInfo ci) {
        if (selectedTab == EduTabs.CHEMISTRY) this.edu$applyCategory();
    }

    @Inject(method = "extractBackground", at = @At("TAIL"))
    private void edu$drawSideTabs(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (selectedTab == EduTabs.CHEMISTRY) CreativeCategories.render(graphics, this.leftPos, this.topPos, mouseX, mouseY);
        else CreativeCategories.shownArea = null;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void edu$clickSideTabs(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (selectedTab == EduTabs.CHEMISTRY && CreativeCategories.click(this.leftPos, this.topPos, event.x(), event.y())) {
            this.edu$applyCategory();
            this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            cir.setReturnValue(true);
        }
    }
}
