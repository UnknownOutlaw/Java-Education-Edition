package edu.chemistry;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.serialization.Codec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;

/**
 * Bedrock's "Education Edition" world toggle, as a game rule ({@code edu:education_features}). It is off by default,
 * like Bedrock; the world creation screen has a button for it and it can be changed later with /gamerule.
 */
public final class EduRules {
    public static GameRule<Boolean> EDUCATION_FEATURES;
    /** the rule's value in the world this client is connected to (sent by the server, used to show or hide the creative tab) */
    public static volatile boolean clientEnabled = false;
    /** run (on the client thread) whenever {@link #clientEnabled} changes; recipe viewers use it to show or hide the items */
    public static Runnable onClientEnabledChanged = () -> {
    };
    /** Bedrock's "Immutable World": only World Builders can build, except above Allow blocks */
    public static GameRule<Boolean> IMMUTABLE_WORLD;
    public static volatile boolean clientImmutable = false;

    public static void init() {
        EDUCATION_FEATURES = Registry.register(BuiltInRegistries.GAME_RULE, EduMod.id("education_features"),
            new GameRule<>(GameRuleCategory.MISC, GameRuleType.BOOL, BoolArgumentType.bool(), GameRuleTypeVisitor::visitBoolean,
                Codec.BOOL, b -> b ? 1 : 0, false, FeatureFlagSet.of()));
        IMMUTABLE_WORLD = Registry.register(BuiltInRegistries.GAME_RULE, EduMod.id("immutable_world"),
            new GameRule<>(GameRuleCategory.PLAYER, GameRuleType.BOOL, BoolArgumentType.bool(), GameRuleTypeVisitor::visitBoolean,
                Codec.BOOL, b -> b ? 1 : 0, false, FeatureFlagSet.of()));
    }

    /** Server worlds read the rule; anything else (client prediction, the recipe book) is allowed and the server decides. */
    public static boolean enabled(Level level) {
        return !(level instanceof ServerLevel server) || server.getGameRules().get(EDUCATION_FEATURES);
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

    /** Vanilla's game-rule packet carrying just this rule, so clients know whether to show the Chemistry tab. */
    public static net.minecraft.network.protocol.game.ClientboundGameRuleValuesPacket syncPacket(boolean enabled) {
        return syncPacket("education_features", enabled);
    }

    /**
     * Vanilla's game-rule packet carrying one of our values. Besides the two rules it also carries "edu:world_builder",
     * sent only to the player it is about (the client only reads these into our fields).
     */
    public static net.minecraft.network.protocol.game.ClientboundGameRuleValuesPacket syncPacket(String name, boolean value) {
        return new net.minecraft.network.protocol.game.ClientboundGameRuleValuesPacket(java.util.Map.of(
            net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.GAME_RULE, EduMod.id(name)),
            Boolean.toString(value)));
    }

    public static void tellDisabled(ServerPlayer player) {
        player.sendOverlayMessage(Component.translatable("message.edu.disabled").withStyle(ChatFormatting.RED));
    }

    private EduRules() {
    }
}
