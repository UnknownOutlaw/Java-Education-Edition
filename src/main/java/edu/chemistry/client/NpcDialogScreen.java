package edu.chemistry.client;

import edu.chemistry.entity.NpcEntity;
import edu.chemistry.entity.NpcNetwork;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.util.FormattedCharSequence;

/**
 * What a student sees when talking to an NPC (Bedrock's npc_screen student view): the NPC's face, its name and dialog
 * in a speech bubble, and its buttons underneath. Opening and closing it triggers the NPC's On Enter / On Exit commands.
 */
public class NpcDialogScreen extends Screen {
    static final int PANEL_BG = 0xF0313131;
    static final int PANEL_EDGE = 0xFF1E1E1E;
    static final int BUBBLE = 0xFFE6E6E6;
    static final int BUBBLE_TEXT = 0xFF303030;
    private static final int W = 320;
    private final NpcEntity npc;
    private final List<NpcEntity.NpcAction> actions;
    private List<FormattedCharSequence> lines = List.of();
    private int scroll;
    private int panelX;
    private int panelY;
    private int panelH;
    private int textX;
    private int textY;
    private int textW;
    private int textH;
    private boolean closed;

    public NpcDialogScreen(NpcEntity npc) {
        super(Component.literal(npc.getNpcName()));
        this.npc = npc;
        this.actions = npc.getActions();
        send(npc, NpcNetwork.OPEN);
    }

    static void send(NpcEntity npc, String... lines) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;
        String[] l = {lines[0], lines.length > 1 ? lines[1] : "", lines.length > 2 ? lines[2] : "", lines.length > 3 ? lines[3] : ""};
        mc.getConnection().send(new ServerboundSignUpdatePacket(NpcNetwork.posFor(npc), true, l[0], l[1], l[2], l[3]));
    }

    /** the skin's face (and hat layer), drawn at {@code scale} screen pixels per skin pixel */
    static void drawFace(GuiGraphicsExtractor g, String skin, int x, int y, int scale) {
        var tex = NpcRenderer.skinTexture(skin);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 8.0F, 8.0F, 8 * scale, 10 * scale, 8, 10, 64, 64);
        g.blit(RenderPipelines.GUI_TEXTURED, tex, x, y, 40.0F, 8.0F, 8 * scale, 8 * scale, 8, 8, 64, 64);
    }

    @Override
    protected void init() {
        List<NpcEntity.NpcAction> buttons = new ArrayList<>();
        for (NpcEntity.NpcAction a : this.actions) if (a.url() || a.mode().equals(NpcEntity.NpcAction.BUTTON)) buttons.add(a);
        int rows = (buttons.size() + 1) / 2;
        this.textW = W - 88 - 16;
        this.textH = 70;
        this.panelH = 24 + this.textH + 12 + rows * 24 + 8;
        this.panelX = (this.width - W) / 2;
        this.panelY = Math.max(4, this.height - this.panelH - 30);
        this.textX = this.panelX + 88 + 6;
        this.textY = this.panelY + 24 + 4;
        this.lines = this.font.split(FormattedText.of(this.npc.getDialog()), this.textW - 8);

        int bw = (W - 24 - 6) / 2;
        for (int i = 0; i < buttons.size(); i++) {
            NpcEntity.NpcAction a = buttons.get(i);
            int index = this.actions.indexOf(a);
            int bx = this.panelX + 12 + (i % 2) * (bw + 6);
            int by = this.panelY + 24 + this.textH + 12 + (i / 2) * 24;
            Component label = Component.literal(a.name().isBlank() ? (a.url() ? "Link" : "Button " + (i + 1)) : a.name());
            this.addRenderableWidget(Button.builder(label, b -> {
                if (a.url()) {
                    String url = a.text().trim();
                    if (url.startsWith("http://") || url.startsWith("https://")) ConfirmLinkScreen.confirmLinkNow(this, url);
                } else {
                    send(this.npc, NpcNetwork.BUTTON + index);
                }
            }).bounds(bx, by, bw, 20).build());
        }
        this.addRenderableWidget(Button.builder(Component.literal("×"), b -> this.onClose())
            .bounds(this.panelX + W - 18, this.panelY + 4, 14, 14).build());
        // World Builders can switch to the editor from the dialog
        if (Minecraft.getInstance().player != null && edu.chemistry.EduPermissions.isWorldBuilder(Minecraft.getInstance().player)) this.addRenderableWidget(Button.builder(Component.translatable("npcscreen.editdialog"), b ->
            Minecraft.getInstance().setScreen(new NpcEditScreen(this.npc)))
            .bounds(this.panelX + W - 18 - 4 - 70, this.panelY + 3, 70, 16).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractBackground(g, mouseX, mouseY, a);
        int x = this.panelX;
        int y = this.panelY;
        g.fill(x - 1, y - 1, x + W + 1, y + this.panelH + 1, PANEL_EDGE);
        g.fill(x, y, x + W, y + this.panelH, PANEL_BG);
        // portrait
        g.fill(x + 8, y + 24, x + 8 + 72, y + 24 + this.textH, 0xFF8BB0D0);
        drawFace(g, this.npc.getSkin(), x + 8 + 12, y + 24 + 5, 6);
        // speech bubble with a little point toward the NPC
        g.fill(this.textX, this.textY - 4, this.textX + this.textW, this.textY - 4 + this.textH, BUBBLE);
        g.fill(this.textX - 4, this.textY + 10, this.textX, this.textY + 18, BUBBLE);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        g.text(this.font, this.title, this.panelX + 8, this.panelY + 8, 0xFFFFFFFF, true);
        int visible = (this.textH - 8) / this.font.lineHeight;
        g.enableScissor(this.textX, this.textY - 4, this.textX + this.textW, this.textY - 4 + this.textH);
        for (int i = 0; i < visible && i + this.scroll < this.lines.size(); i++) {
            g.text(this.font, this.lines.get(i + this.scroll), this.textX + 4, this.textY + i * this.font.lineHeight, BUBBLE_TEXT, false);
        }
        g.disableScissor();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double dx, double dy) {
        int visible = (this.textH - 8) / this.font.lineHeight;
        int max = Math.max(0, this.lines.size() - visible);
        this.scroll = Math.max(0, Math.min(max, this.scroll - (int) Math.signum(dy)));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        if (!this.closed) {
            this.closed = true;
            send(this.npc, NpcNetwork.CLOSE);
        }
        super.removed();
    }

    static Font font() {
        return Minecraft.getInstance().font;
    }
}
