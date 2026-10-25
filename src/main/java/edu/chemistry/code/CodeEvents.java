package edu.chemistry.code;

import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Game events for MakeCode programs, in Education's format (only sent when a program is listening). */
public final class CodeEvents {
    /** the player's movement since the last travel event */
    static final class Tracker {
        Vec3 last;
        double travelled;
        int method = -1;
        int sinceEvent;
        double lastVy;
    }

    private static JsonObject block(BlockState state) {
        BedrockNames.Ref ref = BedrockNames.bedrock(state);
        JsonObject b = new JsonObject();
        b.addProperty("id", ref.id());
        b.addProperty("aux", ref.aux());
        b.addProperty("namespace", "minecraft");
        JsonObject body = new JsonObject();
        body.add("block", b);
        return body;
    }

    private static JsonObject item(ItemStack stack) {
        BedrockNames.Ref ref = BedrockNames.bedrock(stack.getItem());
        JsonObject i = new JsonObject();
        i.addProperty("id", ref.id());
        i.addProperty("aux", ref.aux());
        i.addProperty("namespace", "minecraft");
        return i;
    }

    public static void chat(ServerPlayer player, String message) {
        if (!CodeConnection.wants(player, "PlayerMessage")) return;
        JsonObject body = new JsonObject();
        body.addProperty("type", "chat");
        body.addProperty("message", message);
        body.addProperty("sender", BedrockCommands.playerName(player));
        body.addProperty("receiver", "");
        CodeConnection.event(player, "PlayerMessage", body);
    }

    public static void blockBroken(ServerPlayer player, BlockState state) {
        if (CodeConnection.wants(player, "BlockBroken")) CodeConnection.event(player, "BlockBroken", block(state));
    }

    public static void blockPlaced(ServerPlayer player, BlockState state) {
        if (CodeConnection.wants(player, "BlockPlaced")) CodeConnection.event(player, "BlockPlaced", block(state));
    }

    public static void died(ServerPlayer player) {
        if (CodeConnection.wants(player, "PlayerDied")) CodeConnection.event(player, "PlayerDied", new JsonObject());
    }

    public static void mobKilled(ServerPlayer killer, LivingEntity victim) {
        if (victim instanceof Player || !CodeConnection.wants(killer, "MobKilled")) return;
        JsonObject v = new JsonObject();
        v.addProperty("type", BedrockNames.bedrock(victim.getType()));
        v.addProperty("id", victim.getId());
        JsonObject body = new JsonObject();
        body.add("victim", v);
        CodeConnection.event(killer, "MobKilled", body);
    }

    /** right-clicking with an item (not placing it) */
    public static void itemInteracted(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty() || !CodeConnection.wants(player, "ItemInteracted")) return;
        JsonObject body = new JsonObject();
        body.addProperty("method", 0);
        body.add("item", item(stack));
        CodeConnection.event(player, "ItemInteracted", body);
    }

    /** a bow fired (MakeCode's "on arrow shot") */
    public static void arrowShot(ServerPlayer player, ItemStack bow) {
        if (!CodeConnection.wants(player, "ItemUsed")) return;
        JsonObject body = new JsonObject();
        body.addProperty("useMethod", 5);
        body.add("item", item(bow));
        CodeConnection.event(player, "ItemUsed", body);
    }

    /** every tick while a program listens: how the player travels, teleports and bounces */
    static void tick(ServerPlayer player, Tracker t) {
        Vec3 pos = player.position();
        double vy = player.getDeltaMovement().y;
        if (t.last == null) {
            t.last = pos;
            t.lastVy = vy;
            return;
        }
        double moved = pos.distanceTo(t.last);
        t.last = pos;
        t.sinceEvent++;
        if (moved > 10.0) {
            CodeConnection.event(player, "PlayerTeleported", new JsonObject());
            travelled(player, 11);
            t.travelled = 0;
            t.lastVy = vy;
            return;
        }
        // bouncing off a slime block: falling, then going up again with slime underfoot
        if (t.lastVy < -0.1 && vy > 0.1 && player.level().getBlockState(player.blockPosition().below()).is(Blocks.SLIME_BLOCK)) {
            CodeConnection.event(player, "PlayerBounced", new JsonObject());
            travelled(player, 9);
        }
        t.lastVy = vy;
        int method = method(player);
        if (method != t.method) {
            t.method = method;
            t.travelled = 0;
        }
        t.travelled += moved;
        if (t.travelled >= 1.0 && t.sinceEvent >= 5) {
            t.travelled = 0;
            t.sinceEvent = 0;
            travelled(player, method);
        }
    }

    private static void travelled(ServerPlayer player, int method) {
        if (!CodeConnection.wants(player, "PlayerTravelled")) return;
        JsonObject body = new JsonObject();
        body.addProperty("travelMethod", method);
        body.addProperty("isUnderwater", player.isUnderWater());
        CodeConnection.event(player, "PlayerTravelled", body);
    }

    /** Bedrock's travel methods: 0 walk, 1 swim, 2 fall, 3 climb, 4 swim in lava, 5 fly, 6 ride, 7 sneak, 8 sprint */
    private static int method(ServerPlayer player) {
        if (player.isPassenger()) return 6;
        if (player.getAbilities().flying || player.isFallFlying()) return 5;
        if (player.isInLava()) return 4;
        if (player.isInWater()) return 1;
        if (player.onClimbable() && !player.onGround()) return 3;
        if (!player.onGround()) return 2;
        if (player.isShiftKeyDown()) return 7;
        if (player.isSprinting()) return 8;
        return 0;
    }

    private CodeEvents() {
    }
}
