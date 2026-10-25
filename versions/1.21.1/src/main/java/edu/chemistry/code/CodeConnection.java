package edu.chemistry.code;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.chemistry.EduRules;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

/**
 * The server's side of Code Builder. A player's MakeCode editor tabs (on the player's own computer) send MakeCode's
 * packets through the player's game in {@link CodePayload}s; they're answered here in Education's websocket format,
 * and game events the program subscribed to are sent back the same way.
 */
public final class CodeConnection {
    /** sent with events: every editor tab the player has open gets them */
    public static final int ALL_TABS = -1;
    private static final String EVENT_ID = "00000000-0000-0000-0000-000000000000";

    /** what one player's programs are listening to, and answers waiting to be sent */
    static final class Session {
        final Set<String> subscribed = new HashSet<>();
        final List<Later> later = new ArrayList<>();
        final CodeEvents.Tracker tracker = new CodeEvents.Tracker();
    }

    private record Later(long tick, int tab, JsonObject message) {
    }

    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    private static Session session(ServerPlayer player) {
        return SESSIONS.computeIfAbsent(player.getUUID(), id -> new Session());
    }

    static @Nullable Session existing(ServerPlayer player) {
        return SESSIONS.get(player.getUUID());
    }

    // ---------------------------------------------------------------- receiving

    /** a Code Builder packet from a player's game (on the server thread) */
    public static void receive(ServerPlayer player, String json) {
        JsonObject wrapper;
        try {
            wrapper = JsonParser.parseString(json).getAsJsonObject();
        } catch (RuntimeException e) {
            return;
        }
        if (wrapper.has("reset")) {
            // the player closed their last editor tab: their programs have stopped
            SESSIONS.remove(player.getUUID());
            return;
        }
        int tab = wrapper.has("t") ? wrapper.get("t").getAsInt() : 0;
        JsonElement m = wrapper.get("m");
        if (m == null || !m.isJsonObject()) return;
        handle(player, tab, m.getAsJsonObject());
    }

    private static void handle(ServerPlayer player, int tab, JsonObject msg) {
        JsonObject header = msg.has("header") && msg.get("header").isJsonObject() ? msg.getAsJsonObject("header") : new JsonObject();
        JsonObject body = msg.has("body") && msg.get("body").isJsonObject() ? msg.getAsJsonObject("body") : new JsonObject();
        String purpose = string(header, "messagePurpose", "");
        String requestId = string(header, "requestId", "");
        switch (purpose) {
            case "subscribe" -> session(player).subscribed.add(string(body, "eventName", ""));
            case "unsubscribe" -> session(player).subscribed.remove(string(body, "eventName", ""));
            case "commandRequest", "action:agent" -> {
                if (!body.has("commandLine")) {
                    // MakeCode first asks in an older format to find out which one the game speaks
                    send(player, tab, reply(requestId, "error", BedrockCommands.Result.fail(-2147418111, "Malformed request")));
                    return;
                }
                String line = string(body, "commandLine", "");
                if (!EduRules.enabled(player.serverLevel())) {
                    send(player, tab, reply(requestId, "commandResponse", BedrockCommands.Result.fail(
                        "Education Edition is switched off in this world")));
                    return;
                }
                if (purpose.equals("action:agent")) agentAction(player, tab, requestId, line);
                else send(player, tab, reply(requestId, "commandResponse", BedrockCommands.run(player, line)));
            }
            case "data:file" -> send(player, tab, reply(requestId, "commandResponse",
                BedrockCommands.Result.fail("Reading and writing files isn't available in Java")));
            default -> {
                // telemetry and other messages for Education's own app: nothing to do
            }
        }
    }

    private static void agentAction(ServerPlayer player, int tab, String requestId, String line) {
        List<String> t = BedrockCommands.tokens(line.trim());
        // MakeCode reuses one header for all its requests, so once the Agent has acted, every later request says
        // "action:agent": anything that isn't an Agent action (agent create, tp, setitem, setblock...) is an ordinary command
        if (t.size() < 2 || !t.get(0).equalsIgnoreCase("agent") || !AgentActions.isAction(t.get(1))) {
            send(player, tab, reply(requestId, "commandResponse", BedrockCommands.run(player, line)));
            return;
        }
        if (AgentActions.agentFor(player) == null) {
            send(player, tab, reply(requestId, "commandResponse", BedrockCommands.Result.fail("Cannot issue command, Agent is out of range")));
            return;
        }
        AgentActions.Outcome out = AgentActions.act(player, t.subList(1, t.size()));
        if (out == null) {
            send(player, tab, reply(requestId, "commandResponse", BedrockCommands.Result.fail(BedrockCommands.FAILED_TO_PARSE,
                "Syntax error: " + line)));
            return;
        }
        // first "request accepted", then (when the Agent's step is over) what happened
        send(player, tab, reply(requestId, "commandResponse", BedrockCommands.Result.ok("Agent " + out.name())));
        JsonObject header = new JsonObject();
        header.addProperty("requestId", requestId);
        header.addProperty("messagePurpose", "action:agent");
        header.addProperty("version", 2);
        header.addProperty("action", out.action());
        header.addProperty("actionName", out.name());
        JsonObject message = new JsonObject();
        message.add("header", header);
        message.add("body", out.body());
        long now = player.getServer().getTickCount();
        session(player).later.add(new Later(now + out.delay(), tab, message));
    }

    // ---------------------------------------------------------------- sending

    static JsonObject reply(String requestId, String purpose, BedrockCommands.Result result) {
        JsonObject header = new JsonObject();
        header.addProperty("requestId", requestId);
        header.addProperty("messagePurpose", purpose);
        header.addProperty("version", 2);
        JsonObject body = new JsonObject();
        body.addProperty("statusCode", result.status());
        body.addProperty("statusMessage", result.message());
        for (Map.Entry<String, JsonElement> e : result.extra().entrySet()) body.add(e.getKey(), e.getValue());
        JsonObject message = new JsonObject();
        message.add("header", header);
        message.add("body", body);
        return message;
    }

    static void send(ServerPlayer player, int tab, JsonObject message) {
        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("t", tab);
        wrapper.add("m", message);
        player.connection.send(new ClientboundCustomPayloadPacket(new CodePayload(wrapper.toString())));
    }

    /** is any of this player's programs listening for this event? */
    public static boolean wants(ServerPlayer player, String eventName) {
        Session s = SESSIONS.get(player.getUUID());
        return s != null && s.subscribed.contains(eventName);
    }

    /** sends a game event to the player's programs, if they subscribed to it */
    public static void event(ServerPlayer player, String eventName, JsonObject body) {
        if (!wants(player, eventName)) return;
        JsonObject header = new JsonObject();
        header.addProperty("requestId", EVENT_ID);
        header.addProperty("messagePurpose", "event");
        header.addProperty("version", 2);
        header.addProperty("eventName", eventName);
        JsonObject message = new JsonObject();
        message.add("header", header);
        message.add("body", body);
        send(player, ALL_TABS, message);
    }

    // ---------------------------------------------------------------- every tick

    public static void tick(MinecraftServer server) {
        if (SESSIONS.isEmpty()) return;
        long now = server.getTickCount();
        Iterator<Map.Entry<UUID, Session>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Session> e = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(e.getKey());
            if (player == null) {
                it.remove();
                continue;
            }
            Session s = e.getValue();
            for (Iterator<Later> l = s.later.iterator(); l.hasNext(); ) {
                Later later = l.next();
                if (later.tick() > now) continue;
                l.remove();
                send(player, later.tab(), later.message());
            }
            if (!s.subscribed.isEmpty()) CodeEvents.tick(player, s.tracker);
        }
    }

    /** a new world starts with no one connected */
    public static void reset() {
        SESSIONS.clear();
    }

    private static String string(JsonObject o, String key, String fallback) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : fallback;
    }

    private CodeConnection() {
    }
}
