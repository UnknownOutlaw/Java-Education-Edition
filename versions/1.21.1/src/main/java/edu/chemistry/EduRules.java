package edu.chemistry;

import edu.chemistry.network.EduSyncPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * Bedrock's "Education Edition" world toggle, as a game rule ({@code edu:education_features}). It is off by default,
 * like Bedrock; the world creation screen has a button for it and it can be changed later with /gamerule.
 */
public final class EduRules {
    public static GameRules.Key<GameRules.BooleanValue> EDUCATION_FEATURES;
    /** the rule's value in the world this client is connected to (sent by the server, used to show or hide the creative tab) */
    public static volatile boolean clientEnabled = false;
    /** run (on the client thread) whenever {@link #clientEnabled} changes; recipe viewers use it to show or hide the items */
    public static Runnable onClientEnabledChanged = () -> {
    };
    /** Bedrock's "Immutable World": only World Builders can build, except above Allow blocks */
    public static GameRules.Key<GameRules.BooleanValue> IMMUTABLE_WORLD;
    public static volatile boolean clientImmutable = false;

    public static void init() {
        // when a rule changes (/gamerule), every player's client is told
        EDUCATION_FEATURES = GameRules.register("edu:education_features", GameRules.Category.MISC,
            GameRules.BooleanValue.create(false, (server, value) -> server.getPlayerList().broadcastAll(syncPacket(value.get()))));
        IMMUTABLE_WORLD = GameRules.register("edu:immutable_world", GameRules.Category.PLAYER,
            GameRules.BooleanValue.create(false, (server, value) -> server.getPlayerList().broadcastAll(syncPacket("immutable_world", value.get()))));
    }

    /** Server worlds read the rule; anything else (client prediction, the recipe book) is allowed and the server decides. */
    public static boolean enabled(Level level) {
        return !(level instanceof ServerLevel server) || server.getGameRules().getBoolean(EDUCATION_FEATURES);
    }

    public static boolean isEdu(Item item) {
        return EduMod.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace());
    }

    public static boolean isEdu(ItemStack stack) {
        return !stack.isEmpty() && isEdu(stack.getItem());
    }

    public static boolean isEdu(Block block) {
        return EduMod.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace());
    }

    /** The packet telling a client whether Education Edition is on (to show or hide the Chemistry tab). */
    public static Packet<?> syncPacket(boolean enabled) {
        return syncPacket("education_features", enabled);
    }

    /**
     * The packet carrying one of our values to a client: the two rules, and "world_builder", sent only to the player
     * it is about.
     */
    public static Packet<?> syncPacket(String name, boolean value) {
        return edu.chemistry.network.EduNetwork.toClient.apply(new EduSyncPayload(name, value));
    }

    /** on the client: one of the values the server sends (see {@link #syncPacket(String, boolean)}) */
    public static void clientSync(String name, boolean value) {
        switch (name) {
            case "education_features" -> setClientEnabled(value);
            case "immutable_world" -> clientImmutable = value;
            case "world_builder" -> EduPermissions.clientWorldBuilder = value;
            default -> {
            }
        }
    }

    /** on the client: Education Edition is on or off in this world (shows or hides the Chemistry tab) */
    public static void setClientEnabled(boolean enabled) {
        if (clientEnabled == enabled) return;
        clientEnabled = enabled;
        onClientEnabledChanged.run();
        // make the creative screen rebuild its tabs (and search) next time it looks
        edu.chemistry.registry.EduTabs.refresh();
    }

    public static void tellDisabled(ServerPlayer player) {
        player.displayClientMessage(Component.translatable("message.edu.disabled").withStyle(ChatFormatting.RED), true);
    }

    private EduRules() {
    }
}
