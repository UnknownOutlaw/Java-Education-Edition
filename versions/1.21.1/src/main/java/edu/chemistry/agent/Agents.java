package edu.chemistry.agent;

import edu.chemistry.EduMod;
import edu.chemistry.registry.EduEntities;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Keeps track of whose Agent is whose. Each player has at most one Agent; the player remembers it (and where it was
 * last) in two tags, so it's found again after the player or the Agent has been away. While its player is online the
 * Agent keeps the chunks around it loaded, so it keeps working even far from its player.
 */
public final class Agents {
    /** player tag: "edu_agent:" + the Agent's UUID */
    private static final String AGENT_TAG = "edu_agent:";
    /** player tag: "edu_agent_at|" + dimension + "|" + chunk x + "|" + chunk z */
    private static final String WHERE_TAG = "edu_agent_at|";
    /** how long (ticks) a teleport waits for an Agent whose chunk is still loading */
    private static final int PENDING_TICKS = 100;
    public static final TicketType<ChunkPos> TICKET = TicketType.create("edu_agent", java.util.Comparator.comparingLong(ChunkPos::toLong), 100);

    /** teleports waiting for an Agent to load, by owner */
    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    /** when each player's Agent was first looked for and not found (it may still be loading) */
    private static final Map<UUID, Long> MISSING = new HashMap<>();

    private record Pending(ResourceKey<Level> dimension, Vec3 pos, Direction facing, long until) {
    }

    public static void init() {
        // 1.21.1's ticket types aren't registered: TICKET is made above
    }

    private static void keepLoaded(ServerLevel level, ChunkPos chunk) {
        level.getChunkSource().addRegionTicket(TICKET, chunk, 2, chunk);
    }

    // ---------------------------------------------------------------- tags

    private static @Nullable UUID agentId(ServerPlayer player) {
        for (String tag : player.getTags()) {
            if (!tag.startsWith(AGENT_TAG)) continue;
            try {
                return UUID.fromString(tag.substring(AGENT_TAG.length()));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }

    private static void setAgentId(ServerPlayer player, @Nullable UUID id) {
        player.getTags().stream().filter(t -> t.startsWith(AGENT_TAG)).toList().forEach(player::removeTag);
        if (id != null) player.addTag(AGENT_TAG + id);
    }

    private static @Nullable String where(ServerPlayer player) {
        for (String tag : player.getTags()) {
            if (tag.startsWith(WHERE_TAG)) return tag;
        }
        return null;
    }

    private static void setWhere(ServerPlayer player, @Nullable String tag) {
        String old = where(player);
        if (old != null && old.equals(tag)) return;
        if (old != null) player.removeTag(old);
        if (tag != null) player.addTag(tag);
    }

    private static String whereTag(ServerLevel level, BlockPos pos) {
        return WHERE_TAG + level.dimension().location() + "|" + (pos.getX() >> 4) + "|" + (pos.getZ() >> 4);
    }

    /** loads the chunks where the player's Agent was last seen (so it shows up and can be found) */
    private static boolean loadLastSeen(ServerPlayer player) {
        String tag = where(player);
        if (tag == null) return false;
        String[] parts = tag.substring(WHERE_TAG.length()).split("\\|");
        if (parts.length != 3) return false;
        try {
            ResourceLocation dim = ResourceLocation.parse(parts[0]);
            ServerLevel level = player.level().getServer().getLevel(ResourceKey.create(Registries.DIMENSION, dim));
            if (level == null) return false;
            keepLoaded(level, new ChunkPos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    // ---------------------------------------------------------------- finding

    /** the player's Agent, if it's loaded */
    public static @Nullable AgentEntity find(ServerPlayer player) {
        UUID id = agentId(player);
        if (id == null) return null;
        for (ServerLevel level : player.level().getServer().getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity instanceof AgentEntity agent && agent.isAlive() && agent.isOwner(player)) return agent;
        }
        return null;
    }

    /** what looking for a player's Agent found: the Agent, or that it's still loading, or that there isn't one */
    public record Lookup(@Nullable AgentEntity agent, boolean loading) {
    }

    /**
     * Looks for the player's Agent. One that isn't loaded gets its chunks loaded; one that still hasn't turned up a few
     * seconds later is gone (removed while its player was away), and the player is free to get a new one.
     */
    public static Lookup lookup(ServerPlayer player) {
        UUID owner = player.getUUID();
        AgentEntity agent = find(player);
        if (agent != null) {
            MISSING.remove(owner);
            return new Lookup(agent, false);
        }
        if (agentId(player) == null) return new Lookup(null, false);
        long now = player.level().getServer().getTickCount();
        Long since = MISSING.putIfAbsent(owner, now);
        if ((since != null && now - since > PENDING_TICKS) || !loadLastSeen(player)) {
            MISSING.remove(owner);
            PENDING.remove(owner);
            setAgentId(player, null);
            setWhere(player, null);
            return new Lookup(null, false);
        }
        return new Lookup(null, true);
    }

    /**
     * Brings the player's Agent to {@code pos} facing {@code facing}, making one if the player hasn't got one. When the
     * Agent is somewhere not loaded right now, its chunk is loaded and it comes over as soon as it's there.
     */
    public static boolean summon(ServerPlayer player, ServerLevel level, Vec3 pos, Direction facing) {
        Lookup found = lookup(player);
        long now = level.getServer().getTickCount();
        if (found.agent() != null) {
            PENDING.remove(player.getUUID());
            return teleport(found.agent(), level, pos, facing);
        }
        if (found.loading()) {
            PENDING.put(player.getUUID(), new Pending(level.dimension(), pos, facing, now + PENDING_TICKS));
            return true;
        }
        // no Agent yet: make one
        AgentEntity agent = EduEntities.AGENT.create(level);
        if (agent == null) return false;
        agent.setOwner(player);
        agent.moveTo(pos.x, pos.y, pos.z, facing.toYRot(), 0.0F);
        if (!level.addFreshEntity(agent)) return false;
        PENDING.remove(player.getUUID());
        setAgentId(player, agent.getUUID());
        setWhere(player, whereTag(level, agent.blockPosition()));
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.ANVIL_LAND, SoundSource.NEUTRAL, 0.5F, 1.0F);
        return true;
    }

    private static boolean teleport(AgentEntity agent, ServerLevel level, Vec3 pos, Direction facing) {
        if (agent.level() == level) {
            agent.setPos(pos);
            agent.face(facing);
            moved(agent);
            return true;
        }
        return agent.teleportTo(level, pos.x, pos.y, pos.z, java.util.Set.of(), facing.toYRot(), 0.0F);
    }

    /** called every tick by each Agent: a teleport may be waiting for it (its chunk was loaded for one) */
    static void applyPending(AgentEntity agent, ServerLevel level) {
        if (PENDING.isEmpty() || agent.owner() == null) return;
        Pending pending = PENDING.get(agent.owner());
        if (pending == null) return;
        PENDING.remove(agent.owner());
        if (pending.until < level.getServer().getTickCount()) return;
        ServerLevel to = level.getServer().getLevel(pending.dimension);
        if (to != null) teleport(agent, to, pending.pos, pending.facing);
    }

    /**
     * Once a second: an Agent its player has replaced (or never had) goes away and leaves its things behind; otherwise
     * it keeps its chunks loaded while its player is online and tells the player where it is.
     */
    static void upkeep(AgentEntity agent, ServerLevel level) {
        UUID owner = agent.owner();
        if (owner == null) return;
        MISSING.remove(owner);
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        if (player == null) return;
        UUID id = agentId(player);
        if (id == null) {
            // the player lost track of it (its tags were cleared): it's theirs again
            setAgentId(player, agent.getUUID());
        } else if (!id.equals(agent.getUUID())) {
            Containers.dropContents(level, agent.blockPosition(), agent.inventory());
            agent.discard();
            return;
        }
        keepLoaded(level, new ChunkPos(agent.blockPosition()));
        setWhere(player, whereTag(level, agent.blockPosition()));
    }

    /** the Agent stepped somewhere new: keep the chunks there loaded too, so it can carry on far from its player */
    static void moved(AgentEntity agent) {
        if (agent.owner() == null || !(agent.level() instanceof ServerLevel level)) return;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(agent.owner());
        if (player == null) return;
        keepLoaded(level, new ChunkPos(agent.blockPosition()));
        setWhere(player, whereTag(level, agent.blockPosition()));
    }

    /** the Agent is gone for good (killed or removed): its player no longer has one */
    static void forget(AgentEntity agent) {
        UUID owner = agent.owner();
        if (owner == null || !(agent.level() instanceof ServerLevel level)) return;
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(owner);
        if (player == null) return;
        UUID id = agentId(player);
        if (id != null && id.equals(agent.getUUID())) {
            setAgentId(player, null);
            setWhere(player, null);
            PENDING.remove(owner);
        }
    }

    /** a player joining loads the chunks where they left their Agent, so it's there to carry on */
    public static void onJoin(ServerPlayer player) {
        loadLastSeen(player);
    }

    /** a new world starts with nothing waiting */
    public static void reset() {
        PENDING.clear();
        MISSING.clear();
    }

    private Agents() {
    }
}
