package edu.chemistry.photo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.chemistry.EduRules;
import edu.chemistry.registry.EduItems;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Portfolio <-> inventory messages from the client, carried in vanilla's sign-update packet (like the chalkboard and
 * NPC messages). Saving a portfolio picture as a photo item uploads its picture in pieces; adding a photo item to the
 * portfolio asks the server to take that item out of the inventory.
 */
public final class PhotoNetwork {
    public static final String UPLOAD = "edu:photo_up:";
    public static final String TAKE = "edu:photo_take:";
    private static final int CHUNK = 384;
    private static final int MAX_CHUNKS = 400;
    private static final Map<UUID, String[]> PENDING = new HashMap<>();

    /** the packet line sets that turn a portfolio picture into a photo item: {marker, data, data, data} */
    public static List<String[]> encodeUpload(String id, byte[] png, String caption) {
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        o.addProperty("caption", caption);
        o.addProperty("png", Base64.getEncoder().encodeToString(png));
        String json = o.toString();
        int per = CHUNK * 3;
        int total = Math.max(1, (json.length() + per - 1) / per);
        List<String[]> out = new ArrayList<>();
        for (int seq = 0; seq < total; seq++) {
            String[] lines = {UPLOAD + seq + ":" + total, "", "", ""};
            for (int i = 0; i < 3; i++) {
                int from = seq * per + i * CHUNK;
                if (from < json.length()) lines[i + 1] = json.substring(from, Math.min(json.length(), from + CHUNK));
            }
            out.add(lines);
        }
        return out;
    }

    /** @return true if this was a photo message */
    public static boolean handle(ServerPlayer player, BlockPos pos, List<String> lines) {
        if (lines.isEmpty()) return false;
        String head = lines.get(0);
        if (head.startsWith(UPLOAD)) {
            if (EduRules.enabled(player.level())) upload(player, head.substring(UPLOAD.length()), lines);
            return true;
        }
        if (head.startsWith(TAKE)) {
            if (EduRules.enabled(player.level())) take(player, head.substring(TAKE.length()));
            return true;
        }
        return false;
    }

    private static void upload(ServerPlayer player, String header, List<String> lines) {
        String[] parts = header.split(":");
        if (parts.length != 2) return;
        int seq;
        int total;
        try {
            seq = Integer.parseInt(parts[0]);
            total = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return;
        }
        if (total < 1 || total > MAX_CHUNKS || seq < 0 || seq >= total) return;
        String[] buf = PENDING.get(player.getUUID());
        if (buf == null || buf.length != total || seq == 0) {
            buf = new String[total];
            PENDING.put(player.getUUID(), buf);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < lines.size(); i++) sb.append(lines.get(i));
        buf[seq] = sb.toString();
        for (String s : buf) if (s == null) return;
        PENDING.remove(player.getUUID());
        try {
            JsonObject o = JsonParser.parseString(String.join("", buf)).getAsJsonObject();
            byte[] png = Base64.getDecoder().decode(o.get("png").getAsString());
            if (png.length > Photos.MAX_BYTES || !Photos.isPng(png) && !Photos.isJpeg(png)) return;
            String caption = o.has("caption") ? o.get("caption").getAsString() : "";
            if (caption.length() > 1000) caption = caption.substring(0, 1000);
            String id = o.has("id") ? o.get("id").getAsString() : UUID.randomUUID().toString();
            ItemStack photo = new ItemStack(EduItems.PHOTO);
            Photos.write(photo, id, png, caption);
            if (!player.getInventory().add(photo)) player.drop(photo, false);
            player.containerMenu.broadcastChanges();
        } catch (RuntimeException e) {
            // bad upload: ignore it
        }
    }

    private static void take(ServerPlayer player, String args) {
        String[] parts = args.split(":", 2);
        if (parts.length != 2) return;
        int slot;
        try {
            slot = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return;
        }
        if (slot < 0 || slot >= player.getInventory().getContainerSize()) return;
        ItemStack stack = player.getInventory().getItem(slot);
        if (!stack.is(EduItems.PHOTO) || !Photos.id(stack).equals(parts[1])) return;
        stack.shrink(1);
        player.containerMenu.broadcastChanges();
    }

    private PhotoNetwork() {
    }
}
