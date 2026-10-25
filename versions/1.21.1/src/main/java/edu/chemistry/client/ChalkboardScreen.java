package edu.chemistry.client;

import edu.chemistry.block.ChalkboardBlock;
import edu.chemistry.block.ChalkboardBlockEntity;
import edu.chemistry.block.ChalkboardEdit;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;

/**
 * The chalkboard editor: a dark panel titled with the board's name (Slate, Poster or Board) and a close cross, a text
 * box shaped like the chalkboard (it wraps exactly where the chalkboard does) and, for World Builders, a vanilla
 * "Locked: ON/OFF" button. Closing it saves.
 */
public class ChalkboardScreen extends Screen {
    private static final int PANEL_BG = 0xFF313131;
    private static final int PANEL_EDGE = 0xFF1E1E1E;
    private static final int LABEL = 0xFFFFFFFF;
    private static final int LABEL_DIM = 0xFFC0C0C0;
    private static final int CHALK = 0xFFF0F0EC;
    private final BlockPos pos;
    private final ChalkboardBlock.Size size;
    private final boolean builder;
    private String text;
    private boolean locked;
    private boolean saved;
    private MultiLineEditBox box;
    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;
    private int boxX;
    private int boxY;
    private int boxW;
    private int boxH;

    public ChalkboardScreen(BlockPos pos) {
        super(Component.translatable("chalkboardScreen.header"));
        this.pos = pos;
        Minecraft mc = Minecraft.getInstance();
        ChalkboardBlockEntity be = mc.level == null ? null : ChalkboardBlock.entity(mc.level, pos);
        this.size = be == null ? ChalkboardBlock.Size.SLATE : be.size();
        this.text = be == null ? "" : be.getText();
        this.locked = be != null && be.isLocked();
        this.builder = mc.player != null && ChalkboardBlock.isWorldBuilder(mc.player);
    }

    private Component boardName() {
        return Component.translatable("block.edu." + this.size.name);
    }

    @Override
    protected void init() {
        this.boxW = this.size.textWidth + 8;
        this.boxH = this.size.lines * this.font.lineHeight + 8;
        this.panelW = Math.max(this.boxW + 20, 120);
        this.panelH = 22 + this.boxH + 8 + (this.builder ? 30 : 2);
        this.panelX = (this.width - this.panelW) / 2;
        this.panelY = (this.height - this.panelH) / 2;
        this.boxX = this.panelX + (this.panelW - this.boxW) / 2;
        this.boxY = this.panelY + 22;

        this.box = new StyledTextBox(this.font, this.boxX, this.boxY, this.boxW, this.boxH, Component.empty(), this.title, CHALK, true, true);
        this.box.setCharacterLimit(ChalkboardBlockEntity.MAX_LENGTH);
        this.box.setValue(this.text);
        // 1.21.1's box has no line limit: text that would wrap past the chalkboard's last line is taken back
        this.box.setValueListener(s -> {
            if (this.lineCount(s) <= this.size.lines) this.text = s;
            else if (!s.equals(this.text)) this.box.setValue(this.text);
        });
        this.addRenderableWidget(this.box);

        // the lock is a vanilla on/off button, like the ones in Options, centred under the box
        if (this.builder) {
            int bw = Math.min(150, this.panelW - 16);
            this.addRenderableWidget(CycleButton.onOffBuilder(this.locked).create(
                this.panelX + (this.panelW - bw) / 2, this.boxY + this.boxH + 6, bw, 20,
                Component.translatable("chalkboardScreen.lock"), (button, value) -> this.locked = value));
        }
        this.setInitialFocus(this.box);
    }

    /** how many lines the text takes in the box, wrapped the way the box wraps it */
    private int lineCount(String s) {
        int lines = this.font.getSplitter().splitLines(s, this.boxW - 8, net.minecraft.network.chat.Style.EMPTY).size();
        return s.isEmpty() || s.endsWith("\n") ? lines + 1 : lines;
    }

    private boolean overClose(double mx, double my) {
        int cx = this.panelX + this.panelW - 13;
        int cy = this.panelY + 4;
        return mx >= cx - 2 && mx < cx + 9 && my >= cy - 2 && my < cy + 10;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (this.overClose(mouseX, mouseY)) {
                this.onClose();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float a) {
        super.renderBackground(g, mouseX, mouseY, a);
        g.fill(this.panelX - 1, this.panelY - 1, this.panelX + this.panelW + 1, this.panelY + this.panelH + 1, PANEL_EDGE);
        g.fill(this.panelX, this.panelY, this.panelX + this.panelW, this.panelY + this.panelH, PANEL_BG);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float a) {
        GuiCompat.blend();
        super.render(g, mouseX, mouseY, a);
        Component name = this.boardName();
        g.drawCenteredString(this.font, name, this.width / 2, this.panelY + 7, LABEL);
        // close cross
        int cx = this.panelX + this.panelW - 13;
        int cy = this.panelY + 4;
        boolean hot = this.overClose(mouseX, mouseY);
        g.drawString(this.font, "\u00d7", cx, cy, hot ? LABEL : LABEL_DIM, true);
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
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;
        String[] lines = ChalkboardEdit.encode(this.text, this.locked);
        mc.getConnection().send(new ServerboundSignUpdatePacket(this.pos, true, lines[0], lines[1], lines[2], lines[3]));
    }
}
