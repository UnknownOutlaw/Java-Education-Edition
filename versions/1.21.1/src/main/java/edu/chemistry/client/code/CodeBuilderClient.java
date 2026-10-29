package edu.chemistry.client.code;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import edu.chemistry.EduMod;
import edu.chemistry.EduRules;
import edu.chemistry.code.CodePayload;
import java.io.IOException;
import java.net.URI;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.Util;

/**
 * Code Builder on the player's computer: the C key (Education's Code Builder key) opens the MakeCode editor in the
 * browser, and MakeCode's messages are passed between the editor tabs and the world the player is in.
 */
public final class CodeBuilderClient {
    public static final KeyMapping KEY = new KeyMapping("key.edu.code_builder", com.mojang.blaze3d.platform.InputConstants.KEY_C,
        "key.categories.misc");
    private static final String PLAYER_JOIN = "{\"header\":{\"requestId\":\"00000000-0000-0000-0000-000000000000\","
        + "\"messagePurpose\":\"event\",\"version\":2,\"eventName\":\"%s\"},\"body\":{}}";

    private static LocalEditorServer server;
    private static boolean inWorld;

    private static final LocalEditorServer.Listener LISTENER = new LocalEditorServer.Listener() {
        @Override
        public void opened(int tab) {
        }

        @Override
        public void message(int tab, String text) {
            Minecraft.getInstance().execute(() -> fromEditor(tab, text));
        }

        @Override
        public void closed(int tab) {
            Minecraft.getInstance().execute(() -> {
                // the last tab closed: the programs it was running have stopped
                if (server != null && !server.hasTabs()) toServer("{\"reset\":true}");
            });
        }
    };

    /** every client tick: the key, and telling open editors when a world is joined or left */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        while (KEY.consumeClick()) open();
        boolean now = mc.getConnection() != null && mc.player != null;
        if (now != inWorld) {
            inWorld = now;
            if (server != null) server.send(-1, String.format(PLAYER_JOIN, now ? "PlayerJoin" : "PlayerLeave"));
        }
    }

    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (!EduRules.clientEnabled) {
            mc.player.displayClientMessage(Component.translatable("message.edu.disabled").withStyle(ChatFormatting.RED), true);
            return;
        }
        try {
            if (server == null) server = new LocalEditorServer(LISTENER);
            server.start();
        } catch (IOException e) {
            EduMod.LOG.error("Code Builder couldn't start its local server", e);
            mc.player.sendSystemMessage(Component.translatable("message.edu.code_builder.failed").withStyle(ChatFormatting.RED));
            return;
        }
        String url = server.url();
        Util.getPlatform().openUri(URI.create(url));
        Component link = Component.literal(url).withStyle(Style.EMPTY.withColor(ChatFormatting.AQUA).withUnderlined(true)
            .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("message.edu.code_builder.link"))));
        mc.player.sendSystemMessage(Component.translatable("message.edu.code_builder.opened", link));
    }

    // ---------------------------------------------------------------- editor -> world

    private static void fromEditor(int tab, String text) {
        JsonObject msg;
        try {
            msg = JsonParser.parseString(text).getAsJsonObject();
        } catch (RuntimeException e) {
            return;
        }
        JsonObject header = msg.has("header") && msg.get("header").isJsonObject() ? msg.getAsJsonObject("header") : new JsonObject();
        String purpose = header.has("messagePurpose") ? header.get("messagePurpose").getAsString() : "";
        if (purpose.startsWith("data:telemetry")) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null || mc.player == null) {
            // not in a world: answer right away so the program doesn't wait forever
            if (purpose.equals("commandRequest") || purpose.equals("action:agent") || purpose.equals("data:file")) {
                String id = header.has("requestId") ? header.get("requestId").getAsString() : "";
                server.send(tab, "{\"header\":{\"requestId\":" + quote(id) + ",\"messagePurpose\":\"commandResponse\",\"version\":2},"
                    + "\"body\":{\"statusCode\":-2147352576,\"statusMessage\":\"Open a world to run your code\"}}");
            }
            return;
        }
        JsonObject wrapper = new JsonObject();
        wrapper.addProperty("t", tab);
        wrapper.add("m", msg);
        toServer(wrapper.toString());
    }

    private static void toServer(String json) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) mc.getConnection().send(edu.chemistry.network.EduNetwork.toServer.apply(new CodePayload(json)));
    }

    // ---------------------------------------------------------------- world -> editor

    /** a Code Builder packet from the server (on the client thread) */
    public static void fromServer(String json) {
        if (server == null) return;
        try {
            JsonObject wrapper = JsonParser.parseString(json).getAsJsonObject();
            int tab = wrapper.has("t") ? wrapper.get("t").getAsInt() : -1;
            JsonElement m = wrapper.get("m");
            if (m != null) server.send(tab, m.toString());
        } catch (RuntimeException e) {
            // not ours
        }
    }

    /** an event this computer sees first (taking a picture with the camera) */
    public static void localEvent(String eventName) {
        if (server != null && server.hasTabs()) server.send(-1, String.format(PLAYER_JOIN, eventName));
    }

    private static String quote(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private CodeBuilderClient() {
    }
}
