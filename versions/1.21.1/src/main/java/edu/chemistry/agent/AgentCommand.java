package edu.chemistry.agent;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import edu.chemistry.EduRules;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Education's /agent command: tells your Agent what to do, one step at a time. Anyone can use it on their own Agent
 * (World Builder isn't needed); it works for players, and from command blocks and functions through /execute as.
 * The first /agent command a player uses makes their Agent next to them.
 * <ul>
 * <li>create, tp [destination] [y-rot]: bring the Agent (making it if needed)</li>
 * <li>move, turn, attack, destroy, place, till, collect, drop, dropall, transfer: do something</li>
 * <li>detect, detectredstone, inspect, inspectdata, getitemcount, getitemspace, getitemdetail, getposition: find
 * something out (the answer is also the command's result, for /execute store)</li>
 * <li>setitem: put items in the Agent's inventory (operators only, like give)</li>
 * </ul>
 * Directions are forward, back, left, right, up and down, from where the Agent faces. Slots are 1 to 27.
 */
public final class AgentCommand {
    private interface Action {
        boolean run(AgentEntity agent) throws CommandSyntaxException;
    }

    /** a question's answer: what to show, and the number the command returns */
    private record Answer(String text, int value) {
    }

    private interface Question {
        @Nullable Answer ask(AgentEntity agent) throws CommandSyntaxException;
    }

    private interface DirectionalAction {
        boolean run(AgentEntity agent, AgentDirection direction, CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException;
    }

    private interface DirectionalQuestion {
        Answer ask(AgentEntity agent, AgentDirection direction);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("agent");

        root.then(Commands.literal("create").executes(ctx -> bring(ctx.getSource(), "createagent", null, null)));
        root.then(Commands.literal("tp").executes(ctx -> bring(ctx.getSource(), "tpagent", null, null))
            .then(Commands.argument("destination", BlockPosArgument.blockPos())
                .executes(ctx -> bring(ctx.getSource(), "tpagent", BlockPosArgument.getBlockPos(ctx, "destination"), null))
                .then(Commands.argument("yRot", FloatArgumentType.floatArg())
                    .executes(ctx -> bring(ctx.getSource(), "tpagent", BlockPosArgument.getBlockPos(ctx, "destination"),
                        FloatArgumentType.getFloat(ctx, "yRot"))))));

        root.then(directions(Commands.literal("move"), "move", (agent, d, ctx) -> agent.move(d)));
        root.then(Commands.literal("turn")
            .then(Commands.literal("left").executes(ctx -> act(ctx.getSource(), "turn", agent -> {
                agent.turn(false);
                return true;
            })))
            .then(Commands.literal("right").executes(ctx -> act(ctx.getSource(), "turn", agent -> {
                agent.turn(true);
                return true;
            }))));
        root.then(directions(Commands.literal("attack"), "attack", (agent, d, ctx) -> agent.attack(d)));
        root.then(directions(Commands.literal("destroy"), "destroy", (agent, d, ctx) -> agent.destroy(d)));
        root.then(directions(Commands.literal("till"), "till", (agent, d, ctx) -> agent.till(d)));
        root.then(Commands.literal("place").then(directions(slot(), "place", (agent, d, ctx) -> agent.place(slot(ctx), d))));
        root.then(Commands.literal("collect")
            .then(Commands.literal("all").executes(ctx -> act(ctx.getSource(), "collect", agent -> agent.collect(null))))
            .then(Commands.argument("item", ItemArgument.item(context))
                .executes(ctx -> act(ctx.getSource(), "collect", agent -> agent.collect(ItemArgument.getItem(ctx, "item").getItem())))));
        root.then(Commands.literal("drop").then(slot().then(directions(Commands.argument("quantity", IntegerArgumentType.integer(1, 64)),
            "drop", (agent, d, ctx) -> agent.drop(slot(ctx), IntegerArgumentType.getInteger(ctx, "quantity"), d)))));
        root.then(directions(Commands.literal("dropall"), "dropall", (agent, d, ctx) -> agent.dropAll(d)));
        root.then(Commands.literal("transfer").then(Commands.argument("srcSlotNum", IntegerArgumentType.integer(1, AgentEntity.SLOTS))
            .then(Commands.argument("quantity", IntegerArgumentType.integer(1, 64))
                .then(Commands.argument("dstSlotNum", IntegerArgumentType.integer(1, AgentEntity.SLOTS))
                    .executes(ctx -> act(ctx.getSource(), "transfer", agent -> agent.transfer(
                        IntegerArgumentType.getInteger(ctx, "srcSlotNum") - 1, IntegerArgumentType.getInteger(ctx, "quantity"),
                        IntegerArgumentType.getInteger(ctx, "dstSlotNum") - 1)))))));

        root.then(questions(Commands.literal("detect"), "detect", (agent, d) -> yesNo(agent.detect(d))));
        root.then(questions(Commands.literal("detectredstone"), "detectredstone", (agent, d) -> yesNo(agent.detectRedstone(d))));
        root.then(questions(Commands.literal("inspect"), "inspect", (agent, d) -> new Answer(agent.inspect(d), 1)));
        root.then(questions(Commands.literal("inspectdata"), "inspectdata", (agent, d) -> new Answer(agent.inspectData(d), 1)));
        root.then(Commands.literal("getitemcount").then(slot().executes(ctx -> ask(ctx.getSource(), "getitemcount", agent -> {
            int n = agent.inventory().getItem(slot(ctx)).getCount();
            return new Answer(Integer.toString(n), n);
        }))));
        root.then(Commands.literal("getitemspace").then(slot().executes(ctx -> ask(ctx.getSource(), "getitemspace", agent -> {
            ItemStack stack = agent.inventory().getItem(slot(ctx));
            int n = stack.isEmpty() ? 64 : stack.getMaxStackSize() - stack.getCount();
            return new Answer(Integer.toString(n), n);
        }))));
        root.then(Commands.literal("getitemdetail").then(slot().executes(ctx -> ask(ctx.getSource(), "getitemdetail", agent -> {
            ItemStack stack = agent.inventory().getItem(slot(ctx));
            String id = AgentEntity.shortId(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            return new Answer(stack.isEmpty() ? "air" : stack.getCount() + " " + id, stack.isEmpty() ? 0 : 1);
        }))));
        root.then(Commands.literal("getposition").executes(ctx -> ask(ctx.getSource(), "getposition", agent -> {
            BlockPos pos = agent.blockPosition();
            return new Answer(pos.getX() + ", " + pos.getY() + ", " + pos.getZ(), 1);
        })));

        // like /give, making items is for operators
        root.then(Commands.literal("setitem").requires(s -> s.hasPermission(2))
            .then(slot().then(Commands.argument("item", ItemArgument.item(context))
                .then(Commands.argument("count", IntegerArgumentType.integer(0, 64))
                    .executes(ctx -> setItem(ctx))
                    .then(Commands.argument("data", IntegerArgumentType.integer(0))
                        .executes(ctx -> setItem(ctx)))))));

        dispatcher.register(root);
    }

    // ---------------------------------------------------------------- argument helpers

    private static ArgumentBuilder<CommandSourceStack, ?> slot() {
        return Commands.argument("slotNum", IntegerArgumentType.integer(1, AgentEntity.SLOTS));
    }

    /** the slot argument, 1-27, as an inventory index */
    private static int slot(CommandContext<CommandSourceStack> ctx) {
        return IntegerArgumentType.getInteger(ctx, "slotNum") - 1;
    }

    /** adds forward / back / left / right / up / down under {@code parent} */
    private static ArgumentBuilder<CommandSourceStack, ?> directions(ArgumentBuilder<CommandSourceStack, ?> parent, String name,
                                                                    DirectionalAction action) {
        for (AgentDirection d : AgentDirection.values()) {
            parent.then(Commands.literal(d.word).executes(ctx -> act(ctx.getSource(), name, agent -> action.run(agent, d, ctx))));
        }
        return parent;
    }

    private static ArgumentBuilder<CommandSourceStack, ?> questions(ArgumentBuilder<CommandSourceStack, ?> parent, String name,
                                                                   DirectionalQuestion question) {
        for (AgentDirection d : AgentDirection.values()) {
            parent.then(Commands.literal(d.word).executes(ctx -> ask(ctx.getSource(), name, agent -> question.ask(agent, d))));
        }
        return parent;
    }

    private static Answer yesNo(boolean yes) {
        return new Answer(Boolean.toString(yes), yes ? 1 : 0);
    }

    // ---------------------------------------------------------------- running

    /** the player's Agent, made beside them the first time; null (after saying why) when there's none to use */
    private static @Nullable AgentEntity agentFor(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!EduRules.enabled(source.getLevel())) {
            source.sendFailure(Component.translatable("message.edu.disabled"));
            return null;
        }
        Agents.Lookup found = Agents.lookup(player);
        if (found.agent() != null) return found.agent();
        if (found.loading()) {
            source.sendFailure(Component.translatable("commands.agent.outofrange"));
            return null;
        }
        ServerLevel level = (ServerLevel) player.level();
        if (!Agents.summon(player, level, Vec3.atBottomCenterOf(player.blockPosition()), player.getDirection())) {
            source.sendFailure(Component.translatable("commands.agent.createagent.failed"));
            return null;
        }
        return Agents.find(player);
    }

    private static int act(CommandSourceStack source, String name, Action action) throws CommandSyntaxException {
        AgentEntity agent = agentFor(source);
        if (agent == null) return 0;
        if (action.run(agent)) {
            source.sendSuccess(() -> Component.translatable("commands.agent." + name + ".success"), false);
            return 1;
        }
        agent.shrug();
        source.sendFailure(Component.translatable("commands.agent." + name + ".failed"));
        return 0;
    }

    private static int ask(CommandSourceStack source, String name, Question question) throws CommandSyntaxException {
        AgentEntity agent = agentFor(source);
        if (agent == null) return 0;
        Answer answer = question.ask(agent);
        if (answer == null) {
            source.sendFailure(Component.translatable("commands.agent." + name + ".failed"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.edu.agent.result",
            Component.translatable("commands.agent." + name + ".success"), answer.text()), false);
        return answer.value();
    }

    /** create and tp: bring the Agent to the player (or to a spot), facing the player's way (or the given turn) */
    private static int bring(CommandSourceStack source, String name, @Nullable BlockPos to, @Nullable Float yRot) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        if (!EduRules.enabled(source.getLevel())) {
            source.sendFailure(Component.translatable("message.edu.disabled"));
            return 0;
        }
        ServerLevel level = source.getLevel();
        BlockPos pos = to != null ? to : player.blockPosition();
        Direction facing = yRot != null ? Direction.fromYRot(yRot) : player.getDirection();
        boolean ok = !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos)
            && Agents.summon(player, level, Vec3.atBottomCenterOf(pos), facing);
        if (!ok) {
            AgentEntity agent = Agents.find(player);
            if (agent != null) agent.shrug();
            source.sendFailure(Component.translatable("commands.agent." + name + ".failed"));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.agent." + name + ".success"), false);
        return 1;
    }

    private static int setItem(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        int count = IntegerArgumentType.getInteger(ctx, "count");
        ItemStack stack = count == 0 ? ItemStack.EMPTY : ItemArgument.getItem(ctx, "item").createItemStack(count, false);
        int slot = slot(ctx);
        return act(ctx.getSource(), "setitem", agent -> {
            agent.inventory().setItem(slot, stack.copy());
            return true;
        });
    }

    private AgentCommand() {
    }
}
