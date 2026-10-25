package edu.chemistry.client;

import edu.chemistry.block.ChalkboardBlock;
import edu.chemistry.entity.BalloonEntity;
import edu.chemistry.entity.NpcEntity;
import edu.chemistry.entity.TripodCameraEntity;
import edu.chemistry.item.BalloonItem;
import edu.chemistry.item.CameraItem;
import edu.chemistry.item.GlowStickItem;
import edu.chemistry.item.GuideBookItem;
import edu.chemistry.item.IceBombItem;
import edu.chemistry.item.LitState;
import edu.chemistry.item.NpcSpawnEggItem;
import edu.chemistry.item.PortfolioItem;
import edu.chemistry.item.SparklerItem;
import edu.chemistry.item.SuperFertilizerItem;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Education Edition's control tips, each with the key (or mouse button) it's bound to:
 * <ul>
 * <li>right-hand side: what the buttons do right now, following what you're looking at and holding ("Place" and
 * "Mine" at a block, "Attack" and "Trade" at a villager, "Eat" with food, "Drop" with anything in hand);</li>
 * <li>left-hand side: an "H Control Tips" label at the bottom of the screen; H opens a panel above it with the movement
 * and menu keys (and numbers each hotbar slot with its key), and closes it again;</li>
 * </ul>
 * The right-hand tips sit at the bottom of the screen and move up above the closed captions while there are any. Like the rest of the mod's content they only show in worlds with
 * Education Edition switched on, and they can be switched off in Options → Controls.
 */
public final class ControlTips {
    /** one line: the key, the action's lang key, and whether the key is pressed twice (double-tap jump to fly) */
    public record Tip(KeyMapping key, String action, boolean twice) {
        Tip(KeyMapping key, String action) {
            this(key, action, false);
        }
    }

    /** every action a tip can show, so the build can check they all have a name */
    public static final List<String> ACTIONS = List.of(
        "mine", "break", "attack", "push", "place", "open", "close", "use", "sleep", "write", "pick_block",
        "talk", "trade", "ride", "leash", "unleash", "name", "shear", "milk", "feed", "rotate", "tie_balloon", "release_balloon",
        "set_up_camera", "take_picture", "close_up", "fertilize", "spawn", "ignite", "till",
        "eat", "drink", "block", "draw_bow", "load", "shoot", "throw", "zoom", "toot", "cast", "reel_in", "read",
        "shake", "light", "drop", "dismount", "start_flying", "fly_up", "fly_down", "swim_up", "swim_down", "jump", "sneak",
        "sprint", "forward", "back", "left", "right", "inventory", "chat", "code_builder", "control_tips");
    private static final int MAX = 5;
    private static final int ROW_H = 17;
    private static final ResourceLocation MOUSE_LEFT = ResourceLocation.fromNamespaceAndPath("edu", "textures/gui/control_tips/mouse_left.png");
    private static final ResourceLocation MOUSE_RIGHT = ResourceLocation.fromNamespaceAndPath("edu", "textures/gui/control_tips/mouse_right.png");
    private static final ResourceLocation MOUSE_MIDDLE = ResourceLocation.fromNamespaceAndPath("edu", "textures/gui/control_tips/mouse_middle.png");

    // ------------------------------------------------------------------ the setting
    private static boolean enabled = true;
    private static boolean loaded;
    public static final OptionInstance<Boolean> OPTION = OptionInstance.createBoolean("options.edu.control_tips",
        OptionInstance.cachedConstantTooltip(Component.translatable("options.edu.control_tips.tooltip")), true, value -> {
            enabled = value;
            save();
        });

    private static Path configFile() {
        return Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("education-edition.properties");
    }

    /** reads the saved setting the first time it's needed (the game folder isn't known when mods first load) */
    public static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(configFile())) {
            p.load(r);
        } catch (IOException ignored) {
            // no file yet: tips stay on, like Bedrock
        }
        enabled = !"false".equals(p.getProperty("controlTips"));
        panelOpen = "true".equals(p.getProperty("controlTipsOpen"));
        OPTION.set(enabled);
    }

    private static void save() {
        Properties p = new Properties();
        p.setProperty("controlTips", Boolean.toString(enabled));
        p.setProperty("controlTipsOpen", Boolean.toString(panelOpen));
        try {
            Files.createDirectories(configFile().getParent());
            try (Writer w = Files.newBufferedWriter(configFile())) {
                p.store(w, "Education Edition client settings");
            }
        } catch (IOException ignored) {
            // the setting just won't be remembered
        }
    }

    // ------------------------------------------------------------------ what to show
    /** right-hand side: what the buttons do with the block or mob you're looking at, and with what you're holding */
    public static List<Tip> actions(Minecraft mc, LocalPlayer p) {
        List<Tip> tips = new ArrayList<>();
        var o = mc.options;
        Level level = p.level();
        ItemStack held = p.getMainHandItem();
        boolean creative = p.getAbilities().instabuild;
        boolean useShown = false;
        HitResult hit = mc.hitResult;
        Tip attack = null;
        Tip pick = null;

        if (hit instanceof EntityHitResult eh) {
            Entity e = eh.getEntity();
            if (e instanceof BalloonEntity) attack = new Tip(o.keyAttack, "push");
            else if (e instanceof Boat || e instanceof AbstractMinecart) attack = new Tip(o.keyAttack, "break");
            else if (e instanceof LivingEntity && !(e instanceof NpcEntity)) attack = new Tip(o.keyAttack, "attack");
            String use = entityUse(e, held, p);
            if (use != null) {
                tips.add(new Tip(o.keyUse, use));
                useShown = true;
            }
        } else if (hit instanceof BlockHitResult bh && hit.getType() == HitResult.Type.BLOCK) {
            BlockPos pos = bh.getBlockPos();
            BlockState state = level.getBlockState(pos);
            attack = new Tip(o.keyAttack, creative ? "break" : "mine");
            String use = p.isSecondaryUseActive() ? null : blockUse(state, level, pos);
            if (use == null) use = itemUseOn(held, state, p);
            if (use != null) {
                tips.add(new Tip(o.keyUse, use));
                useShown = true;
            }
            if (creative) pick = new Tip(o.keyPickItem, "pick_block");
        }
        if (!useShown) {
            String use = itemUse(held, p);
            if (use != null) tips.add(new Tip(o.keyUse, use));
        }
        if (attack != null) tips.add(attack);
        if (pick != null) tips.add(pick);
        if (!held.isEmpty()) tips.add(new Tip(o.keyDrop, "drop"));
        while (tips.size() > MAX) tips.remove(tips.size() - 1);
        return tips;
    }

    /** left-hand panel: moving about and the menus */
    public static List<Tip> controls(Minecraft mc, LocalPlayer p) {
        var o = mc.options;
        List<Tip> tips = new ArrayList<>();
        tips.add(new Tip(o.keyUp, "forward"));
        tips.add(new Tip(o.keyDown, "back"));
        tips.add(new Tip(o.keyLeft, "left"));
        tips.add(new Tip(o.keyRight, "right"));
        if (p.isPassenger()) {
            tips.add(new Tip(o.keyShift, "dismount"));
        } else if (p.getAbilities().flying) {
            tips.add(new Tip(o.keyJump, "fly_up"));
            tips.add(new Tip(o.keyShift, "fly_down"));
        } else if (p.isInWater()) {
            tips.add(new Tip(o.keyJump, "swim_up"));
            tips.add(new Tip(o.keyShift, "swim_down"));
        } else {
            tips.add(new Tip(o.keyJump, "jump"));
            if (p.getAbilities().mayfly) tips.add(new Tip(o.keyJump, "start_flying", true));
        }
        tips.add(new Tip(o.keyInventory, "inventory"));
        tips.add(new Tip(o.keyChat, "chat"));
        tips.add(new Tip(edu.chemistry.client.code.CodeBuilderClient.KEY, "code_builder"));
        if (!p.isPassenger() && !p.getAbilities().flying && !p.isInWater()) tips.add(new Tip(o.keyShift, "sneak"));
        tips.add(new Tip(o.keySprint, "sprint"));
        return tips;
    }

    private static String entityUse(Entity e, ItemStack held, LocalPlayer p) {
        if (e instanceof NpcEntity) return "talk";
        if (e instanceof TripodCameraEntity) return "take_picture";
        if (e instanceof AbstractVillager v && !v.isBaby()) return "trade";
        if (held.getItem() instanceof BalloonItem && e instanceof LivingEntity) return "tie_balloon";
        if (e instanceof Leashable l) {
            if (l.isLeashed() && l.getLeashHolder() == p) return "unleash";
            if (held.is(Items.LEAD) && l.canBeLeashed()) return "leash";
        }
        if (held.is(Items.NAME_TAG) && held.has(DataComponents.CUSTOM_NAME) && e instanceof LivingEntity) return "name";
        if (held.is(Items.SHEARS) && e instanceof Sheep s && s.readyForShearing()) return "shear";
        if (held.is(Items.BUCKET) && e instanceof Cow c && !c.isBaby()) return "milk";
        if (e instanceof Animal a && a.isFood(held)) return "feed";
        if (e instanceof AbstractHorse || e instanceof Boat || e instanceof AbstractMinecart) return "ride";
        if (e instanceof ItemFrame) return "rotate";
        return null;
    }

    private static String blockUse(BlockState state, Level level, BlockPos pos) {
        if (state.getBlock() instanceof ChalkboardBlock) return "write";
        if (state.getBlock() instanceof BedBlock) return "sleep";
        if (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof TrapDoorBlock || state.getBlock() instanceof FenceGateBlock) {
            if (state.is(net.minecraft.world.level.block.Blocks.IRON_DOOR) || state.is(net.minecraft.world.level.block.Blocks.IRON_TRAPDOOR)) return null;
            return state.hasProperty(BlockStateProperties.OPEN) && state.getValue(BlockStateProperties.OPEN) ? "close" : "open";
        }
        if (state.getBlock() instanceof ButtonBlock || state.getBlock() instanceof LeverBlock) return "use";
        if (state.getMenuProvider(level, pos) != null) return "open";
        return null;
    }

    private static String itemUseOn(ItemStack held, BlockState state, LocalPlayer p) {
        var item = held.getItem();
        if (item instanceof CameraItem) return p.isShiftKeyDown() ? "close_up" : "set_up_camera";
        if (item instanceof BalloonItem) {
            return state.getBlock() instanceof FenceBlock || state.getBlock() instanceof WallBlock ? "tie_balloon" : "release_balloon";
        }
        if ((item instanceof SuperFertilizerItem || item instanceof BoneMealItem) && state.getBlock() instanceof BonemealableBlock) return "fertilize";
        if (item instanceof SpawnEggItem || item instanceof NpcSpawnEggItem) return "spawn";
        if (held.is(Items.FLINT_AND_STEEL) || held.is(Items.FIRE_CHARGE)) return "ignite";
        if (item instanceof net.minecraft.world.item.HoeItem
            && (state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) || state.is(net.minecraft.world.level.block.Blocks.DIRT))) return "till";
        if (item instanceof BlockItem) return "place";
        return null;
    }

    private static String itemUse(ItemStack held, LocalPlayer p) {
        if (held.isEmpty()) return null;
        var item = held.getItem();
        if (item instanceof CameraItem) return p.isShiftKeyDown() ? "close_up" : "take_picture";
        if (item instanceof PortfolioItem) return "open";
        if (item instanceof GuideBookItem || held.is(Items.WRITTEN_BOOK)) return "read";
        if (held.is(Items.WRITABLE_BOOK)) return "write";
        if (item instanceof GlowStickItem) return LitState.isLit(held) ? null : "shake";
        if (item instanceof SparklerItem) return LitState.isLit(held) ? null : "light";
        if (item instanceof FishingRodItem) return p.fishing != null ? "reel_in" : "cast";
        if (item instanceof CrossbowItem) return CrossbowItem.isCharged(held) ? "shoot" : "load";
        UseAnim anim = held.getUseAnimation();
        if (anim == UseAnim.EAT) {
            FoodProperties food = held.get(DataComponents.FOOD);
            return food != null && p.canEat(food.canAlwaysEat()) ? "eat" : null;
        }
        if (anim == UseAnim.DRINK) return "drink";
        if (anim == UseAnim.BLOCK) return "block";
        if (anim == UseAnim.BOW) return "draw_bow";
        if (anim == UseAnim.SPEAR) return "throw";
        if (anim == UseAnim.SPYGLASS) return "zoom";
        if (anim == UseAnim.TOOT_HORN) return "toot";
        if (item instanceof IceBombItem || held.is(Items.ENDER_EYE)
            || item instanceof ProjectileItem && !(item instanceof ArrowItem) && !(item instanceof FireworkRocketItem)) return "throw";
        return null;
    }

    // ------------------------------------------------------------------ the H key
    public static final KeyMapping TOGGLE_KEY = new KeyMapping("key.edu.control_tips", com.mojang.blaze3d.platform.InputConstants.KEY_H,
        "key.categories.misc");
    private static boolean panelOpen;

    // ------------------------------------------------------------------ drawing
    private static final int TEXT = 0xFFFFFF;
    private static String shown = "";
    private static long changedAt;

    public static void render(GuiGraphics g, int screenW, int screenH) {
        GuiCompat.blend();
        ensureLoaded();
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        while (TOGGLE_KEY.consumeClick()) {
            panelOpen = !panelOpen;
            save();
        }
        if (!enabled || !edu.chemistry.EduRules.clientEnabled || p == null || mc.options.hideGui || mc.screen != null
            || p.isSpectator() || mc.getDebugOverlay().showDebugScreen()) {
            shown = "";
            return;
        }
        Font font = mc.font;
        renderActions(g, font, mc, p, screenW, screenH);
        renderPanel(g, font, mc, p, screenW, screenH);
        if (panelOpen) renderHotbarKeys(g, font, mc, screenW, screenH);
    }

    /** right-hand side, kept clear of the closed captions in the bottom right corner */
    private static void renderActions(GuiGraphics g, Font font, Minecraft mc, LocalPlayer p, int screenW, int screenH) {
        List<Tip> tips = actions(mc, p);
        StringBuilder sig = new StringBuilder();
        for (Tip t : tips) sig.append(t.action()).append(t.key().saveString()).append(';');
        long now = System.currentTimeMillis();
        if (!sig.toString().equals(shown)) {
            shown = sig.toString();
            changedAt = now;
        }
        if (tips.isEmpty()) return;
        float alpha = Math.min(1.0F, (now - changedAt) / 180.0F);
        if (alpha <= 0.02F) return;
        int w = 0;
        for (Tip t : tips) w = Math.max(w, glyphWidth(font, t) + 4 + font.width(label(t)) + 2);
        int h = tips.size() * ROW_H - 3;
        int x = screenW - 6 - w;
        int bottom = screenH - 4;
        if (x < screenW / 2 + 91 + 29 + 4) bottom = screenH - 22 - 4;  // narrow screen: stay above the hotbar
        bottom = Math.min(bottom, captionsTop(mc, screenH) - 3);  // and above the closed captions
        int top = Math.max(4, bottom - h);
        for (int i = 0; i < tips.size(); i++) row(g, font, tips.get(i), x, top + i * ROW_H, alpha);
    }

    /** left-hand side: the "H Control Tips" label, and the panel of controls above it while it's open */
    private static void renderPanel(GuiGraphics g, Font font, Minecraft mc, LocalPlayer p, int screenW, int screenH) {
        Tip toggle = new Tip(TOGGLE_KEY, "control_tips");
        int x = 6;
        // at the bottom of the screen like Education's; on a narrow screen it sits just above the hotbar instead
        int labelW = glyphWidth(font, toggle) + 4 + font.width(label(toggle)) + 2;
        int labelY = x + labelW > screenW / 2 - 91 - 29 - 4 ? screenH - 22 - 18 : screenH - 18;
        List<Tip> tips = panelOpen ? controls(mc, p) : List.of();
        int pad = 5;
        int rowH = ROW_H;
        if (panelOpen) {
            // the open panel has to fit above the label: tighten the rows on very short screens
            int room = labelY - 7 - 2 - pad * 2 + 3;
            if (tips.size() * rowH > room) rowH = Math.max(15, room / tips.size());
        }
        row(g, font, toggle, x, labelY, 1.0F);
        if (!panelOpen) return;
        int w = 0;
        for (Tip t : tips) w = Math.max(w, glyphWidth(font, t) + 4 + font.width(label(t)) + 2);
        int panelW = w + pad * 2;
        int panelH = tips.size() * rowH + pad * 2 - 3;
        int panelBottom = labelY - 7;
        int panelTop = Math.max(2, panelBottom - panelH);
        // a dark see-through panel with a light grey rim and a little point down towards the label
        int rim = 0xFF8B8B8B;
        int bg = 0x88000000;
        g.fill(x + 1, panelTop + 1, x + panelW - 1, panelBottom - 1, bg);
        g.fill(x + 1, panelTop, x + panelW - 1, panelTop + 1, rim);
        g.fill(x + 1, panelBottom - 1, x + panelW - 1, panelBottom, rim);
        g.fill(x, panelTop + 1, x + 1, panelBottom - 1, rim);
        g.fill(x + panelW - 1, panelTop + 1, x + panelW, panelBottom - 1, rim);
        for (int i = 0; i < 4; i++) {
            g.fill(x + 6 + i, panelBottom + i, x + 14 - i, panelBottom + i + 1, i == 3 ? rim : bg);
            g.fill(x + 5 + i, panelBottom + i, x + 6 + i, panelBottom + i + 1, rim);
            g.fill(x + 14 - i, panelBottom + i, x + 15 - i, panelBottom + i + 1, rim);
        }
        for (int i = 0; i < tips.size(); i++) row(g, font, tips.get(i), x + pad, panelTop + pad + i * rowH, 1.0F);
    }

    /** each hotbar slot's number key, in its top left corner */
    private static void renderHotbarKeys(GuiGraphics g, Font font, Minecraft mc, int screenW, int screenH) {
        KeyMapping[] slots = mc.options.keyHotbarSlots;
        int left = screenW / 2 - 91;
        for (int i = 0; i < 9 && i < slots.length; i++) {
            if (slots[i].isUnbound()) continue;
            String k = keyLabel(new Tip(slots[i], "")).toUpperCase(java.util.Locale.ROOT);
            int x = left + i * 20 + 1;
            int y = screenH - 22 + 1;
            g.fill(x, y, x + font.width(k) + 2, y + 9, 0xB0000000);
            g.drawString(font, k, x + 1, y + 1, 0xFFFFFFFF, false);
        }
    }

    /**
     * Where the closed captions' box starts, or the bottom of the screen when they're off or quiet. Vanilla draws caption
     * i centred on screenH - 35 - 10i, each about 11 high, so n of them reach up to screenH - 31 - 10n.
     */
    private static int captionsTop(Minecraft mc, int screenH) {
        if (!mc.options.showSubtitles().get()) return screenH;
        var overlay = ((edu.chemistry.mixin.GuiAccessor) mc.gui).edu$subtitleOverlay();
        int lines = ((edu.chemistry.mixin.SubtitleOverlayAccessor) overlay).edu$subtitles().size();
        if (lines <= 0) return screenH;
        return screenH - 31 - lines * 10;
    }

    private static String label(Tip t) {
        return Component.translatable("controltips.edu." + t.action()).getString();
    }

    /** one tip: the key cap or mouse, then its action on a dark strip */
    private static void row(GuiGraphics g, Font font, Tip t, int x, int y, float alpha) {
        int glyphW = glyphWidth(font, t);
        drawGlyph(g, font, t, x, y, alpha);
        String label = label(t);
        int lx = x + glyphW + 4;
        g.fill(lx - 1, y + 2, lx + font.width(label) + 1, y + 12, argb((int) (0x99 * alpha), 0x000000));
        g.drawString(font, label, lx, y + 3, argb((int) (255 * alpha), TEXT), false);
    }

    private static int argb(int a, int rgb) {
        return (Math.max(0, Math.min(255, a)) << 24) | rgb;
    }

    private static ResourceLocation mouseIcon(Tip t) {
        String k = t.key().saveString();
        if (k.equals("key.mouse.left")) return MOUSE_LEFT;
        if (k.equals("key.mouse.right")) return MOUSE_RIGHT;
        if (k.equals("key.mouse.middle")) return MOUSE_MIDDLE;
        return null;
    }

    private static String keyLabel(Tip t) {
        String s = t.key().isUnbound() ? "?" : t.key().getTranslatedKeyMessage().getString();
        if (s.startsWith("Left ") || s.startsWith("Right ")) s = s.substring(s.indexOf(' ') + 1);
        if (s.equals("Control")) s = "Ctrl";
        s = s.toUpperCase(java.util.Locale.ROOT);
        return t.twice() ? s + "x2" : s;
    }

    private static int glyphWidth(Font font, Tip t) {
        return mouseIcon(t) != null ? 10 : Math.max(14, font.width(keyLabel(t)) + 7);
    }

    /** a grey key cap (light top, darker front edge, dark rim) with the key's name, or the mouse with its button lit */
    private static void drawGlyph(GuiGraphics g, Font font, Tip t, int x, int y, float alpha) {
        ResourceLocation icon = mouseIcon(t);
        if (icon != null) {
            GuiCompat.tinted(g, argb((int) (255 * alpha), 0xFFFFFF), () -> g.blit(icon, x, y, 0.0F, 0.0F, 10, 14, 10, 14));
            return;
        }
        String label = keyLabel(t);
        int w = glyphWidth(font, t);
        int h = 14;
        int a = (int) (255 * alpha);
        g.fill(x + 1, y, x + w - 1, y + h, argb(a, 0x2E2E2E));
        g.fill(x, y + 1, x + w, y + h - 1, argb(a, 0x2E2E2E));
        g.fill(x + 1, y + 1, x + w - 1, y + h - 4, argb(a, 0xC6C6C6));
        g.fill(x + 1, y + h - 4, x + w - 1, y + h - 1, argb(a, 0x8A8A8A));
        g.fill(x + 1, y + 1, x + w - 1, y + 2, argb(a, 0xE8E8E8));
        g.fill(x + 1, y + 1, x + 2, y + h - 4, argb(a, 0xE8E8E8));
        g.drawString(font, label, x + 3, y + 2, argb(a, 0x3A3A3A), false);
    }

    private ControlTips() {
    }
}
