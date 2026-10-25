package edu.chemistry.code;

import com.google.gson.JsonObject;
import edu.chemistry.agent.AgentDirection;
import edu.chemistry.agent.AgentEntity;
import edu.chemistry.agent.Agents;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Agent's actions for MakeCode ("action:agent" requests): each is done at once in the world, and its answer is
 * sent when the Agent's step would be over, so programs move it at Education's pace instead of all in one tick.
 */
public final class AgentActions {
    /** an action's answer: Bedrock's action number and body, and how many ticks to wait before sending it */
    public record Outcome(int action, String name, JsonObject body, int delay) {
    }

    /** the player's Agent, made beside them if they have none; null while it's loading or can't be made */
    static @Nullable AgentEntity agentFor(ServerPlayer player) {
        Agents.Lookup found = Agents.lookup(player);
        if (found.agent() != null) return found.agent();
        if (found.loading()) return null;
        Agents.summon(player, player.serverLevel(), Vec3.atBottomCenterOf(player.blockPosition()), player.getDirection());
        return Agents.find(player);
    }

    private static @Nullable AgentDirection direction(String word) {
        for (AgentDirection d : AgentDirection.values()) {
            if (d.word.equals(word.toLowerCase(Locale.ROOT))) return d;
        }
        return null;
    }

    private static JsonObject result(boolean ok) {
        JsonObject o = new JsonObject();
        o.addProperty("result", ok);
        return o;
    }

    private static final java.util.Set<String> ACTIONS = java.util.Set.of("move", "turn", "attack", "destroy", "till", "interact",
        "dropall", "detect", "detectredstone", "place", "drop", "transfer", "collect", "inspect", "inspectdata", "getitemcount",
        "getitemspace", "getitemdetail");

    /** whether "agent <verb>" is one of the Agent's actions (answered when its step is over), not an ordinary command */
    static boolean isAction(String verb) {
        return ACTIONS.contains(verb.toLowerCase(Locale.ROOT));
    }

    /** does an Agent action; null if the request makes no sense (unknown action or arguments) */
    public static @Nullable Outcome act(ServerPlayer player, List<String> args) {
        AgentEntity agent = agentFor(player);
        if (agent == null) return null;
        String verb = BedrockCommands.arg(args, 0, "").toLowerCase(Locale.ROOT);
        String a1 = BedrockCommands.arg(args, 1, "");
        Outcome out = switch (verb) {
            case "move" -> {
                AgentDirection d = direction(a1);
                if (d == null) yield null;
                boolean moved = agent.move(d);
                JsonObject body = result(moved);
                body.addProperty("status", moved ? 2 : 1);
                yield new Outcome(14, "move", body, 8);
            }
            case "turn" -> {
                boolean right = a1.equalsIgnoreCase("right");
                if (!right && !a1.equalsIgnoreCase("left")) yield null;
                agent.turn(right);
                yield new Outcome(18, "turn", result(true), 6);
            }
            case "attack" -> directional(agent, a1, 1, "attack", 8, agent::attack);
            case "destroy" -> directional(agent, a1, 3, "destroy", 8, agent::destroy);
            case "till" -> directional(agent, a1, 16, "till", 8, agent::till);
            case "interact" -> directional(agent, a1, 13, "interact", 8, agent::interactWith);
            case "dropall" -> directional(agent, a1, 7, "dropAll", 6, agent::dropAll);
            case "detect" -> query(agent, a1, 5, "detectObstacle", agent::detect);
            case "detectredstone" -> query(agent, a1, 4, "detectRedstone", agent::detectRedstone);
            case "place" -> {
                // place slot direction
                AgentDirection d = direction(BedrockCommands.arg(args, 2, ""));
                if (d == null || !BedrockCommands.isInt(a1)) yield null;
                yield new Outcome(15, "placeBlock", result(agent.place(Integer.parseInt(a1) - 1, d)), 8);
            }
            case "drop" -> {
                // drop slot quantity direction
                AgentDirection d = direction(BedrockCommands.arg(args, 3, ""));
                String qty = BedrockCommands.arg(args, 2, "1");
                if (d == null || !BedrockCommands.isInt(a1) || !BedrockCommands.isInt(qty)) yield null;
                yield new Outcome(6, "drop", result(agent.drop(Integer.parseInt(a1) - 1, Math.max(1, Integer.parseInt(qty)), d)), 6);
            }
            case "transfer" -> {
                // transfer from quantity to
                String qty = BedrockCommands.arg(args, 2, "1");
                String to = BedrockCommands.arg(args, 3, "");
                if (!BedrockCommands.isInt(a1) || !BedrockCommands.isInt(qty) || !BedrockCommands.isInt(to)) yield null;
                yield new Outcome(17, "transferItemTo",
                    result(agent.transfer(Integer.parseInt(a1) - 1, Integer.parseInt(qty), Integer.parseInt(to) - 1)), 4);
            }
            case "collect" -> {
                Item only = null;
                if (!a1.equalsIgnoreCase("all")) {
                    String id = BedrockNames.item(a1, 0);
                    if (id == null) yield new Outcome(2, "collect", result(false), 2);
                    only = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
                }
                yield new Outcome(2, "collect", result(agent.collect(only)), 6);
            }
            case "inspect", "inspectdata" -> {
                AgentDirection d = direction(a1);
                if (d == null) yield null;
                BedrockNames.Ref block = BedrockNames.bedrock(player.serverLevel().getBlockState(agent.target(d)));
                JsonObject body = result(true);
                JsonObject b = new JsonObject();
                b.addProperty("id", block.id());
                b.addProperty("aux", block.aux());
                body.add("block", b);
                yield new Outcome(verb.equals("inspect") ? 8 : 9, verb.equals("inspect") ? "inspect" : "inspectData", body, 2);
            }
            case "getitemcount", "getitemspace", "getitemdetail" -> {
                if (!BedrockCommands.isInt(a1)) yield null;
                int slot = Integer.parseInt(a1) - 1;
                if (slot < 0 || slot >= AgentEntity.SLOTS) yield null;
                ItemStack stack = agent.inventory().getItem(slot);
                JsonObject body = result(true);
                JsonObject item = new JsonObject();
                switch (verb) {
                    case "getitemcount" -> item.addProperty("count", stack.getCount());
                    case "getitemspace" -> item.addProperty("freeStackSize", stack.isEmpty() ? 64 : stack.getMaxStackSize() - stack.getCount());
                    default -> {
                        item.addProperty("id", stack.isEmpty() ? "air" : BedrockNames.bedrock(stack.getItem()).id());
                        item.addProperty("aux", stack.isEmpty() ? 0 : BedrockNames.bedrock(stack.getItem()).aux());
                        item.addProperty("stackSize", stack.getCount());
                    }
                }
                body.add("item", item);
                int action = verb.equals("getitemcount") ? 10 : verb.equals("getitemdetail") ? 11 : 12;
                String name = verb.equals("getitemcount") ? "inspectItemCount" : verb.equals("getitemdetail") ? "inspectItemDetail" : "inspectItemSpace";
                yield new Outcome(action, name, body, 1);
            }
            default -> null;
        };
        if (out != null && out.delay() > 2 && out.body().has("result") && !out.body().get("result").getAsBoolean()) agent.shrug();
        return out;
    }

    private interface DirectionalAction {
        boolean run(AgentDirection direction);
    }

    private static @Nullable Outcome directional(AgentEntity agent, String word, int action, String name, int delay, DirectionalAction run) {
        AgentDirection d = direction(word);
        if (d == null) return null;
        return new Outcome(action, name, result(run.run(d)), delay);
    }

    private static @Nullable Outcome query(AgentEntity agent, String word, int action, String name, DirectionalAction run) {
        AgentDirection d = direction(word);
        if (d == null) return null;
        return new Outcome(action, name, result(run.run(d)), 1);
    }

    /** an Agent action that came as a normal command: done, and answered at once */
    static BedrockCommands.Result runNow(ServerPlayer player, List<String> args) {
        Outcome out = act(player, args);
        if (out == null) return BedrockCommands.Result.fail(BedrockCommands.FAILED_TO_PARSE, "Syntax error: agent " + String.join(" ", args));
        BedrockCommands.Result r = BedrockCommands.Result.ok("Agent " + out.name() + " done");
        for (var e : out.body().entrySet()) r.extra().add(e.getKey(), e.getValue());
        return r;
    }

    private AgentActions() {
    }
}
