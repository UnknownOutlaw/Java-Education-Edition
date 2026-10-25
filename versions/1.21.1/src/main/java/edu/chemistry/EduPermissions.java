package edu.chemistry;

import edu.chemistry.registry.EduBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Education Edition's building permissions: World Builders build anywhere; everyone else is stopped by Deny blocks (in
 * the whole column above them), let back in by Allow blocks, kept out of Border columns, and (with the Immutable World
 * rule on) can only build above Allow blocks. The highest Allow or Deny under a spot decides, like Bedrock.
 */
public final class EduPermissions {
    /** player tag set by /wb; tags are saved with the player */
    public static final String WORLD_BUILDER_TAG = "edu_world_builder";
    /** whether this client's player is a World Builder (sent by the server) */
    public static volatile boolean clientWorldBuilder;

    public static boolean isWorldBuilder(Player player) {
        if (player.level().isClientSide()) return player.isLocalPlayer() && clientWorldBuilder;
        return player.getTags().contains(WORLD_BUILDER_TAG);
    }

    /**
     * The world's host: the player who owns the singleplayer / LAN world (or, on a dedicated server, an owner-level
     * operator). Only the host can use World Builder commands; players who join the world can't.
     */
    public static boolean isHost(ServerPlayer player) {
        net.minecraft.server.MinecraftServer server = player.server;
        if (server.isSingleplayerOwner(player.getGameProfile())) return true;
        return server.isDedicatedServer() && server.getProfilePermissions(player.getGameProfile()) >= 4;
    }

    /** World Builder commands: /wb, /worldbuilder and the Education Edition gamerules (also inside /execute ... run) */
    private static final java.util.regex.Pattern WB_COMMAND = java.util.regex.Pattern.compile(
        "(?:^|\\brun\\s+)/?(?:(?:wb|worldbuilder)(?:\\s|$)|gamerule\\s+edu:)", java.util.regex.Pattern.CASE_INSENSITIVE);

    public static boolean isWorldBuilderCommand(String command) {
        return command != null && WB_COMMAND.matcher(command.trim()).find();
    }

    /**
     * The player who actually typed a command, even when it runs through /execute as someone else; null when it came
     * from the console, a command block or a function. (A player's commands carry the player's own command source.)
     */
    public static @org.jspecify.annotations.Nullable ServerPlayer typedBy(net.minecraft.commands.CommandSourceStack source) {
        net.minecraft.commands.CommandSource origin = ((edu.chemistry.mixin.CommandSourceStackAccessor) source).edu$origin();
        if (origin instanceof ServerPlayer player) return player;
        // the game also checks commands with a stand-in source that has no server (to mark them as op-only for clients)
        if (source.getServer() == null) return null;
        return null;
    }

    public static void tellHostOnly(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("commands.edu.host_only").withStyle(ChatFormatting.RED));
    }

    public static boolean isPermissionBlock(Block block) {
        return block == EduBlocks.ALLOW || block == EduBlocks.DENY || block == EduBlocks.BORDER;
    }

    private static boolean featuresOn(Level level) {
        return level instanceof ServerLevel ? EduRules.enabled(level) : EduRules.clientEnabled;
    }

    private static boolean immutable(Level level) {
        return level instanceof ServerLevel server ? server.getGameRules().getBoolean(EduRules.IMMUTABLE_WORLD) : EduRules.clientImmutable;
    }

    /** Can this player place or break a block at {@code pos}? */
    public static boolean canBuild(Level level, Player player, BlockPos pos) {
        if (!featuresOn(level) || isWorldBuilder(player)) return true;
        if (isPermissionBlock(level.getBlockState(pos).getBlock())) return false;
        if (isBorderColumn(level, pos.getX(), pos.getZ())) return false;
        BlockPos.MutableBlockPos p = pos.mutable();
        for (int y = pos.getY() - 1; y >= level.getMinBuildHeight(); y--) {
            p.setY(y);
            BlockState state = level.getBlockState(p);
            if (state.is(EduBlocks.ALLOW)) return true;
            if (state.is(EduBlocks.DENY)) return false;
        }
        return !immutable(level);
    }

    public static void tellCantBuild(Player player) {
        if (player instanceof ServerPlayer sp) {
            sp.displayClientMessage(Component.translatable("message.edu.cant_build").withStyle(ChatFormatting.RED), true);
        }
    }

    /** Is there a Border block anywhere in this column? (Only looks at loaded chunks.) */
    public static boolean isBorderColumn(Level level, int x, int z) {
        if (EduBlocks.BORDER == null || !level.hasChunk(x >> 4, z >> 4)) return false;
        LevelChunk chunk = level.getChunk(x >> 4, z >> 4);
        LevelChunkSection[] sections = chunk.getSections();
        for (LevelChunkSection section : sections) {
            if (section == null || section.hasOnlyAir() || !section.maybeHas(s -> s.is(EduBlocks.BORDER))) continue;
            for (int y = 0; y < 16; y++) {
                if (section.getBlockState(x & 15, y, z & 15).is(EduBlocks.BORDER)) return true;
            }
        }
        return false;
    }

    private EduPermissions() {
    }
}
