package edu.chemistry.block;

import edu.chemistry.EduRules;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Chalkboard edits are sent with vanilla's sign-update packet, so the mod needs no networking library: the first line is
 * a marker plus the lock flag and the other three carry the text (up to 384 characters each).
 */
public final class ChalkboardEdit {
    public static final String MARKER = "edu:chalkboard:";
    public static final int CHUNK = 384;

    /** the four packet lines for a chalkboard's new text and lock */
    public static String[] encode(String text, boolean locked) {
        String[] lines = {MARKER + (locked ? "1" : "0"), "", "", ""};
        for (int i = 0; i < 3; i++) {
            int from = i * CHUNK;
            if (from < text.length()) lines[i + 1] = text.substring(from, Math.min(text.length(), from + CHUNK));
        }
        return lines;
    }

    /** @return true if the packet was a chalkboard edit (handled, or refused) */
    public static boolean handle(ServerPlayer player, BlockPos pos, List<String> lines) {
        if (lines.isEmpty() || !lines.get(0).startsWith(MARKER)) return false;
        ChalkboardBlockEntity be = ChalkboardBlock.entity(player.level(), pos);
        if (be == null || !EduRules.enabled(player.level())) return true;
        if (!player.canInteractWithBlock(pos, 4.0)) return true;
        boolean builder = ChalkboardBlock.isWorldBuilder(player);
        if (be.isLocked() && !builder) {
            player.displayClientMessage(Component.translatable("message.edu.chalkboard_locked").withStyle(ChatFormatting.RED), true);
            return true;
        }
        boolean locked = builder ? lines.get(0).endsWith("1") : be.isLocked();
        StringBuilder text = new StringBuilder();
        for (int i = 1; i < lines.size(); i++) text.append(lines.get(i));
        be.update(text.toString(), locked);
        return true;
    }

    private ChalkboardEdit() {
    }
}
