package edu.chemistry.entity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.chemistry.EduPermissions;
import edu.chemistry.EduRules;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec2;

/**
 * NPC messages from the client, carried (like chalkboard edits) in vanilla's sign-update packet so no networking
 * library is needed. The block position holds the NPC's entity id; the first line says what happened.
 */
public final class NpcNetwork {
    public static final String OPEN = "edu:npc_open";
    public static final String CLOSE = "edu:npc_close";
    public static final String BUTTON = "edu:npc_button:";
    public static final String EDIT = "edu:npc_edit:";
    public static final int CHUNK = 384;
    private static final int MAX_CHUNKS = 64;
    private static final double REACH = 16.0;

    /** half-received edits, by player */
    private static final Map<UUID, String[]> PENDING = new HashMap<>();

    public static BlockPos posFor(NpcEntity npc) {
        return new BlockPos(npc.getId(), 0, 0);
    }

    /** the sign-packet line sets for saving an NPC: each is {marker, text, text, text} */
    public static List<String[]> encodeEdit(int entityId, String name, String skin, String dialog, List<NpcEntity.NpcAction> actions) {
        JsonObject o = new JsonObject();
        o.addProperty("name", name);
        o.addProperty("skin", skin);
        o.addProperty("dialog", dialog);
        o.add("actions", NpcEntity.NpcAction.listToJson(actions));
        String json = o.toString();
        int per = CHUNK * 3;
        int total = Math.max(1, (json.length() + per - 1) / per);
        List<String[]> out = new ArrayList<>();
        for (int seq = 0; seq < total; seq++) {
            String[] lines = {EDIT + entityId + ":" + seq + ":" + total, "", "", ""};
            for (int i = 0; i < 3; i++) {
                int from = seq * per + i * CHUNK;
                if (from < json.length()) lines[i + 1] = json.substring(from, Math.min(json.length(), from + CHUNK));
            }
            out.add(lines);
        }
        return out;
    }

    /** @return true if the packet was an NPC message */
    public static boolean handle(ServerPlayer player, BlockPos pos, List<String> lines) {
        if (lines.isEmpty()) return false;
        String head = lines.get(0);
        if (!head.startsWith("edu:npc_")) return false;
        if (!EduRules.enabled(player.level())) return true;
        ServerLevel level = player.serverLevel();
        Entity e = level.getEntity(pos.getX());
        if (!(e instanceof NpcEntity npc) || player.distanceToSqr(npc) > REACH * REACH) return true;

        if (head.equals(OPEN)) {
            run(player, npc, NpcEntity.NpcAction.ON_ENTER, -1);
        } else if (head.equals(CLOSE)) {
            run(player, npc, NpcEntity.NpcAction.ON_EXIT, -1);
        } else if (head.startsWith(BUTTON)) {
            try {
                run(player, npc, NpcEntity.NpcAction.BUTTON, Integer.parseInt(head.substring(BUTTON.length())));
            } catch (NumberFormatException ignored) {
                // bad packet
            }
        } else if (head.startsWith(EDIT)) {
            edit(player, npc, head, lines);
        }
        return true;
    }

    private static void edit(ServerPlayer player, NpcEntity npc, String head, List<String> lines) {
        if (!EduPermissions.isWorldBuilder(player)) return;
        String[] parts = head.substring(EDIT.length()).split(":");
        if (parts.length != 3) return;
        int seq;
        int total;
        try {
            seq = Integer.parseInt(parts[1]);
            total = Integer.parseInt(parts[2]);
        } catch (NumberFormatException ex) {
            return;
        }
        if (total < 1 || total > MAX_CHUNKS || seq < 0 || seq >= total) return;
        String[] buf = PENDING.get(player.getUUID());
        if (buf == null || buf.length != total || seq == 0) {
            buf = new String[total];
            PENDING.put(player.getUUID(), buf);
        }
        StringBuilder chunk = new StringBuilder();
        for (int i = 1; i < lines.size(); i++) chunk.append(lines.get(i));
        buf[seq] = chunk.toString();
        for (String s : buf) if (s == null) return;
        PENDING.remove(player.getUUID());
        try {
            JsonObject o = JsonParser.parseString(String.join("", buf)).getAsJsonObject();
            String name = o.has("name") ? o.get("name").getAsString() : "NPC";
            npc.setCustomName(Component.literal(name.isBlank() ? "NPC" : name.length() > 64 ? name.substring(0, 64) : name));
            if (o.has("skin")) npc.setSkin(o.get("skin").getAsString());
            if (o.has("dialog")) npc.setDialog(o.get("dialog").getAsString());
            if (o.has("actions")) npc.setActions(NpcEntity.NpcAction.listFromJson(o.get("actions").toString()));
        } catch (RuntimeException ex) {
            // malformed edit: ignore it
        }
    }

    /**
     * Runs an NPC's commands like Bedrock: as the NPC, at the NPC, with @initiator meaning the player who talked to it.
     * Each line of a command action is one command.
     */
    private static void run(ServerPlayer player, NpcEntity npc, String mode, int index) {
        List<NpcEntity.NpcAction> actions = npc.getActions();
        MinecraftServer server = player.level().getServer();
        CommandSourceStack source = server.createCommandSourceStack()
            .withEntity(npc)
            .withPosition(npc.position())
            .withRotation(new Vec2(npc.getXRot(), npc.getYRot()))
            .withLevel(player.serverLevel())
            .withPermission(2)
            .withSuppressedOutput();
        for (int i = 0; i < actions.size(); i++) {
            NpcEntity.NpcAction action = actions.get(i);
            if (action.url() || !action.mode().equals(mode)) continue;
            if (index >= 0 && i != index) continue;
            for (String line : action.text().split("\n")) {
                String command = line.trim();
                if (command.startsWith("/")) command = command.substring(1);
                if (command.isEmpty()) continue;
                command = command.replace("@initiator", player.getName().getString());
                server.getCommands().performPrefixedCommand(source, command);
            }
        }
    }

    private NpcNetwork() {
    }
}
