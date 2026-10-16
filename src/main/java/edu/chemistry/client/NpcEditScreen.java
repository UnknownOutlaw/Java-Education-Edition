package edu.chemistry.client;

import edu.chemistry.entity.NpcEntity;
import edu.chemistry.entity.NpcNetwork;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

/**
 * The World Builder's NPC editor, like Bedrock's: the basic page has the skin picker, the name and the dialog; Advanced
 * Settings lists the NPC's commands and links (each with a button name; commands can also run when the dialog opens or
 * closes). Closing the editor saves.
 */
public class NpcEditScreen extends Screen {
    private static final int W = 340;

    private static final class Action {
        boolean url;
        String name;
        String mode;
        String text;

        Action(NpcEntity.NpcAction a) {
            this.url = a.url();
            this.name = a.name();
            this.mode = a.mode();
            this.text = a.text();
        }

        NpcEntity.NpcAction freeze() {
            return new NpcEntity.NpcAction(this.url, this.name, this.url ? NpcEntity.NpcAction.BUTTON : this.mode, this.text);
        }
    }

    private final NpcEntity npc;
    private String name;
    private String skin;
    private String dialog;
    private final List<Action> actions = new ArrayList<>();
    private boolean advanced;
    private int pack;
    private int page;
    private boolean saved;
    private int panelX;
    private int panelY;
    private int panelH;

    public NpcEditScreen(NpcEntity npc) {
        super(Component.translatable("npcscreen.basictitle"));
        this.npc = npc;
        this.name = npc.getNpcName();
        this.skin = npc.getSkin();
        this.dialog = npc.getDialog();
        for (NpcEntity.NpcAction a : npc.getActions()) this.actions.add(new Action(a));
        for (int p = 0; p < NpcEntity.SKIN_PACKS.length; p++) {
            if (NpcEntity.skinsInPack(p).contains(this.skin)) this.pack = p;
        }
    }

    private int perPage() {
        return Math.max(1, Math.min(4, (this.height - 120) / 62));
    }

    @Override
    protected void init() {
        this.panelH = this.advanced ? 70 + this.perPage() * 62 : 200;
        this.panelX = (this.width - W) / 2;
        this.panelY = Math.max(4, (this.height - this.panelH) / 2);
        if (this.advanced) this.initAdvanced();
        else this.initBasic();
        this.addRenderableWidget(Button.builder(Component.literal("×"), b -> this.onClose())
            .bounds(this.panelX + W - 18, this.panelY + 4, 14, 14).build());
    }

    private void initBasic() {
        int x = this.panelX;
        int y = this.panelY;
        // skin picker: pack arrows and a 5 x 2 grid of faces
        this.addRenderableWidget(Button.builder(Component.literal("<"), b -> {
            this.pack = (this.pack + NpcEntity.SKIN_PACKS.length - 1) % NpcEntity.SKIN_PACKS.length;
            this.rebuildWidgets();
        }).bounds(x + 8, y + 22, 16, 16).build());
        this.addRenderableWidget(Button.builder(Component.literal(">"), b -> {
            this.pack = (this.pack + 1) % NpcEntity.SKIN_PACKS.length;
            this.rebuildWidgets();
        }).bounds(x + 8 + 140 - 16, y + 22, 16, 16).build());
        List<String> skins = NpcEntity.skinsInPack(this.pack);
        for (int i = 0; i < skins.size(); i++) {
            String s = skins.get(i);
            int bx = x + 8 + (i % 5) * 28;
            int by = y + 44 + (i / 5) * 34;
            this.addRenderableWidget(new SkinButton(bx, by, s));
        }

        int rx = x + 160;
        EditBox nameBox = new EditBox(this.font, rx, y + 32, W - 160 - 10, 18, Component.translatable("npcscreen.name"));
        nameBox.setMaxLength(64);
        nameBox.setValue(this.name);
        nameBox.setResponder(s -> this.name = s);
        this.addRenderableWidget(nameBox);

        MultiLineEditBox dialogBox = MultiLineEditBox.builder()
            .setX(rx).setY(y + 66)
            .setPlaceholder(Component.translatable("npcscreen.addtext"))
            .build(this.font, W - 160 - 10, 92, Component.translatable("npcscreen.dialog"));
        dialogBox.setCharacterLimit(1000);
        dialogBox.setValue(this.dialog);
        dialogBox.setValueListener(s -> this.dialog = s);
        this.addRenderableWidget(dialogBox);

        this.addRenderableWidget(Button.builder(Component.translatable("npcscreen.advancedsettings"), b -> {
            this.advanced = true;
            this.rebuildWidgets();
        }).bounds(x + 8, y + this.panelH - 28, 150, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> this.onClose())
            .bounds(x + W - 108, y + this.panelH - 28, 100, 20).build());
    }

    private void initAdvanced() {
        int x = this.panelX;
        int y = this.panelY;
        int per = this.perPage();
        int pages = Math.max(1, (this.actions.size() + per - 1) / per);
        this.page = Math.min(this.page, pages - 1);
        for (int k = 0; k < per; k++) {
            int index = this.page * per + k;
            if (index >= this.actions.size()) break;
            Action a = this.actions.get(index);
            int ry = y + 24 + k * 62;
            EditBox nameBox = new EditBox(this.font, x + 70, ry, 150, 18, Component.translatable("npcscreen.action.buttonname"));
            nameBox.setMaxLength(64);
            nameBox.setValue(a.name);
            nameBox.setHint(Component.translatable("npcscreen.action.buttonname"));
            nameBox.setResponder(s -> a.name = s);
            this.addRenderableWidget(nameBox);
            if (!a.url) {
                this.addRenderableWidget(CycleButton.<String>builder(m -> Component.translatable(switch (m) {
                        case NpcEntity.NpcAction.ON_ENTER -> "npcscreen.action.onEnter";
                        case NpcEntity.NpcAction.ON_EXIT -> "npcscreen.action.onExit";
                        default -> "npcscreen.action.buttonmode";
                    }), a.mode)
                    .withValues(NpcEntity.NpcAction.BUTTON, NpcEntity.NpcAction.ON_ENTER, NpcEntity.NpcAction.ON_EXIT)
                    .displayOnlyValue()
                    .create(x + 224, ry - 1, 84, 20, Component.empty(), (b, v) -> a.mode = v));
            }
            this.addRenderableWidget(Button.builder(Component.literal("✖"), b -> {
                this.actions.remove(a);
                this.rebuildWidgets();
            }).bounds(x + W - 28, ry - 1, 20, 20).build());
            MultiLineEditBox text = MultiLineEditBox.builder()
                .setX(x + 70).setY(ry + 21)
                .setPlaceholder(Component.translatable(a.url ? "npcscreen.action.url.placeholder" : "npcscreen.action.command.placeholder"))
                .build(this.font, W - 70 - 8, 34, Component.translatable(a.url ? "npcscreen.action.url.title" : "npcscreen.action.command.title"));
            text.setCharacterLimit(a.url ? 512 : 2000);
            text.setValue(a.text);
            text.setValueListener(s -> a.text = s);
            this.addRenderableWidget(text);
        }
        int by = y + this.panelH - 28;
        if (pages > 1) {
            this.addRenderableWidget(Button.builder(Component.literal("<"), b -> {
                this.page = Math.max(0, this.page - 1);
                this.rebuildWidgets();
            }).bounds(x + 8, by - 24, 20, 20).build()).active = this.page > 0;
            this.addRenderableWidget(Button.builder(Component.literal(">"), b -> {
                this.page++;
                this.rebuildWidgets();
            }).bounds(x + W - 28, by - 24, 20, 20).build()).active = this.page < pages - 1;
        }
        this.addRenderableWidget(Button.builder(Component.translatable("npcscreen.addcommand"), b -> {
            this.actions.add(new Action(new NpcEntity.NpcAction(false, "", NpcEntity.NpcAction.BUTTON, "")));
            this.page = (this.actions.size() - 1) / per;
            this.rebuildWidgets();
        }).bounds(x + 8, by, 100, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("npcscreen.addurl"), b -> {
            this.actions.add(new Action(new NpcEntity.NpcAction(true, "", NpcEntity.NpcAction.BUTTON, "")));
            this.page = (this.actions.size() - 1) / per;
            this.rebuildWidgets();
        }).bounds(x + 112, by, 100, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> {
            this.advanced = false;
            this.rebuildWidgets();
        }).bounds(x + W - 108, by, 100, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int x = this.panelX;
        int y = this.panelY;
        g.fill(x - 1, y - 1, x + W + 1, y + this.panelH + 1, NpcDialogScreen.PANEL_EDGE);
        g.fill(x, y, x + W, y + this.panelH, NpcDialogScreen.PANEL_BG);
        if (!this.advanced) {
            g.fill(x + 6, y + 42, x + 150, y + 112, 0xFF8BB0D0);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        int x = this.panelX;
        int y = this.panelY;
        Component title = Component.translatable(this.advanced ? "npcscreen.advancedtitle" : "npcscreen.basictitle");
        g.centeredText(this.font, title, x + W / 2, y + 7, 0xFFFFFFFF);
        if (!this.advanced) {
            String packName = NpcEntity.SKIN_PACKS[this.pack][0];
            Component pack = Component.translatable("npcscreen.pack." + packName);
            g.centeredText(this.font, pack, x + 8 + 70, y + 26, 0xFFFFFFFF);
            g.text(this.font, Component.translatable("npcscreen.appearance"), x + 8, y + 120, 0xFFAAAAAA, false);
            drawLarge(g, x + 8, y + 132);
            g.text(this.font, Component.translatable("npcscreen.name"), x + 160, y + 22, 0xFFFFFFFF, false);
            g.text(this.font, Component.translatable("npcscreen.dialog"), x + 160, y + 56, 0xFFFFFFFF, false);
        } else {
            int per = this.perPage();
            for (int k = 0; k < per; k++) {
                int index = this.page * per + k;
                if (index >= this.actions.size()) break;
                Action act = this.actions.get(index);
                int ry = y + 24 + k * 62;
                g.text(this.font, Component.translatable(act.url ? "npcscreen.action.url.title" : "npcscreen.action.command.title"),
                    x + 8, ry + 5, 0xFFFFFFFF, false);
            }
            if (this.actions.isEmpty()) {
                // like Bedrock, each add button has its help text right above it
                int by = y + this.panelH - 28;
                this.helpAbove(g, x + 8, by, "npcscreen.help.command.a", "npcscreen.help.command.b");
                this.helpAbove(g, x + 112, by, "npcscreen.help.url.a", "npcscreen.help.url.b");
            }
        }
    }

    /** help text wrapped to a 100-wide button and drawn so its last line sits just above the button at (x, buttonY) */
    private void helpAbove(GuiGraphicsExtractor g, int x, int buttonY, String first, String second) {
        java.util.List<net.minecraft.util.FormattedCharSequence> lines = new java.util.ArrayList<>();
        lines.addAll(this.font.split(Component.translatable(first), 100));
        lines.addAll(this.font.split(Component.translatable(second), 100));
        int top = buttonY - 4 - lines.size() * this.font.lineHeight;
        for (int i = 0; i < lines.size(); i++) {
            net.minecraft.util.FormattedCharSequence line = lines.get(i);
            g.text(this.font, line, x + (100 - this.font.width(line)) / 2, top + i * this.font.lineHeight, 0xFFAAAAAA, false);
        }
    }

    /** the selected skin's face, bigger, under the grid */
    private void drawLarge(GuiGraphicsExtractor g, int x, int y) {
        NpcDialogScreen.drawFace(g, this.skin, x + 2, y + 2, 4);
        g.text(this.font, Component.literal(this.skin.replace("npc_", "")), x + 40, y + 16, 0xFFDDDDDD, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void onClose() {
        this.save();
        super.onClose();
    }

    @Override
    public void removed() {
        this.save();
        super.removed();
    }

    private void save() {
        if (this.saved) return;
        this.saved = true;
        List<NpcEntity.NpcAction> out = new ArrayList<>();
        for (Action a : this.actions) out.add(a.freeze());
        for (String[] lines : NpcNetwork.encodeEdit(this.npc.getId(), this.name, this.skin, this.dialog, out)) {
            NpcDialogScreen.send(this.npc, lines);
        }
    }

    /** one face in the skin grid */
    private final class SkinButton extends Button {
        private final String skinName;

        SkinButton(int x, int y, String skinName) {
            super(x, y, 26, 32, Component.literal(skinName), b -> NpcEditScreen.this.skin = skinName, DEFAULT_NARRATION);
            this.skinName = skinName;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
            boolean selected = this.skinName.equals(NpcEditScreen.this.skin);
            int border = selected ? 0xFFFFFFFF : this.isHoveredOrFocused() ? 0xFFAAAAAA : 0xFF555555;
            g.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, border);
            g.fill(this.getX() + 1, this.getY() + 1, this.getX() + this.width - 1, this.getY() + this.height - 1, 0xFF6F8FAA);
            NpcDialogScreen.drawFace(g, this.skinName, this.getX() + 5, this.getY() + 1, 2);
        }
    }
}
