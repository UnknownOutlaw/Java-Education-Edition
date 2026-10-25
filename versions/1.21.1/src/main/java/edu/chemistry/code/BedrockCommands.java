package edu.chemistry.code;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import edu.chemistry.EduPermissions;
import edu.chemistry.agent.AgentEntity;
import edu.chemistry.agent.Agents;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Runs MakeCode's command lines (Bedrock syntax) in the Java world and answers the way Education does. Most commands
 * are translated to their Java form and run as the player; the ones MakeCode reads answers from (testforblock,
 * querytarget, gettopsolidblock, the time and weather queries, the Agent's position...) are answered here.
 * <p>
 * Commands run with the player's own permissions; the world's host and World Builders get operator commands, so
 * Code Builder works in a world without cheats for them (like Education, where Code Builder needs no cheats).
 */
public final class BedrockCommands {
    public static final int OK = 0;
    public static final int EXECUTION_FAIL = -2147352576;
    public static final int FAILED_TO_PARSE = -2147483648;
    public static final int INVALID_CALL = -2147483642;

    /** a command's answer: Bedrock's status code and message, plus any extra fields for the reply body */
    public record Result(int status, String message, JsonObject extra) {
        static Result ok(String message) {
            return new Result(OK, message, new JsonObject());
        }

        static Result fail(String message) {
            return new Result(EXECUTION_FAIL, message, new JsonObject());
        }

        static Result fail(int status, String message) {
            return new Result(status, message, new JsonObject());
        }

        Result with(String key, Object value) {
            if (value instanceof Boolean b) this.extra.addProperty(key, b);
            else if (value instanceof Number n) this.extra.addProperty(key, n);
            else if (value instanceof JsonObject o) this.extra.add(key, o);
            else this.extra.addProperty(key, String.valueOf(value));
            return this;
        }
    }

    /** Bedrock's game rules by their 1.21.1 Java names (pvp, tntexplodes and commandblocksenabled came later in Java) */
    static final Map<String, String> GAME_RULES = Map.ofEntries(
        Map.entry("drowningdamage", "drowningDamage"), Map.entry("falldamage", "fallDamage"), Map.entry("firedamage", "fireDamage"),
        Map.entry("freezedamage", "freezeDamage"), Map.entry("dodaylightcycle", "doDaylightCycle"),
        Map.entry("doweathercycle", "doWeatherCycle"), Map.entry("domobloot", "doMobLoot"), Map.entry("domobspawning", "doMobSpawning"),
        Map.entry("mobgriefing", "mobGriefing"), Map.entry("dotiledrops", "doTileDrops"), Map.entry("keepinventory", "keepInventory"),
        Map.entry("naturalregeneration", "naturalRegeneration"), Map.entry("commandblockoutput", "commandBlockOutput"),
        Map.entry("doentitydrops", "doEntityDrops"), Map.entry("sendcommandfeedback", "sendCommandFeedback"),
        Map.entry("showdeathmessages", "showDeathMessages"), Map.entry("doimmediaterespawn", "doImmediateRespawn"),
        Map.entry("randomtickspeed", "randomTickSpeed"), Map.entry("spawnradius", "spawnRadius"),
        Map.entry("maxcommandchainlength", "maxCommandChainLength"), Map.entry("playerssleepingpercentage", "playersSleepingPercentage"),
        Map.entry("doinsomnia", "doInsomnia"));

    private static final Map<String, String> EFFECTS = Map.of("fatal_poison", "poison", "village_hero", "hero_of_the_village");
    private static final Map<String, String> ENCHANTMENTS = Map.of("binding", "binding_curse", "vanishing", "vanishing_curse");

    // ---------------------------------------------------------------- entry

    public static Result run(ServerPlayer player, String commandLine) {
        String line = commandLine.trim();
        if (line.startsWith("/")) line = line.substring(1);
        if (line.isEmpty()) return Result.fail(FAILED_TO_PARSE, "Empty command");
        List<String> t = tokens(line);
        String name = t.get(0).toLowerCase(Locale.ROOT);
        try {
            return switch (name) {
                case "geteduclientinfo" -> Result.ok("").with("isEdu", true).with("companionProtocolVersion", 4).with("isHost", true);
                case "getlocalplayername" -> Result.ok("").with("localplayername", playerName(player));
                case "closechat", "position", "enableencryption" -> Result.ok("");
                case "agent" -> agent(player, t.subList(1, t.size()));
                case "tpagent" -> agent(player, List.of("tp"));
                case "createagent" -> agent(player, List.of("create"));
                case "testforblock" -> testForBlock(player, t);
                case "testforblocks" -> testForBlocks(player, t);
                case "gettopsolidblock" -> topSolidBlock(player, t);
                case "querytarget" -> queryTarget(player, t);
                case "time" -> time(player, t);
                case "weather" -> weather(player, t);
                case "toggledownfall" -> {
                    Level level = player.serverLevel();
                    yield java(player, level.isRaining() ? "weather clear" : "weather rain");
                }
                case "immutableworld" -> {
                    if (!EduPermissions.isHost(player)) yield Result.fail("Only the world's host can change Immutable World");
                    yield java(player, "gamerule edu:immutable_world " + bool(arg(t, 1, "true")));
                }
                case "structure", "getchunks", "getchunkdata", "codebuilder" -> Result.fail(INVALID_CALL, "This command isn't available in Java");
                case "gamerule" -> {
                    if (!GAME_RULES.containsKey(arg(t, 1, "").toLowerCase(Locale.ROOT))) yield Result.ok("Game rule " + arg(t, 1, "") + " isn't in Java");
                    yield java(player, translate(player, line, t));
                }
                default -> {
                    String javaCommand = translate(player, line, t);
                    if (javaCommand == null) yield Result.fail(INVALID_CALL, "Unknown block, item or mob in: " + line);
                    yield java(player, javaCommand);
                }
            };
        } catch (Exception e) {
            return Result.fail(FAILED_TO_PARSE, "Syntax error: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- translating to Java commands

    /** the Java form of a Bedrock command line, or null if it names a block, item or mob Java doesn't have */
    static @Nullable String translate(ServerPlayer player, String line, List<String> t) {
        String name = t.get(0).toLowerCase(Locale.ROOT);
        switch (name) {
            case "setblock": {
                // setblock x y z block [data] [replace|destroy|keep]
                int i = 4;
                String block = arg(t, i++, "air");
                int data = 0;
                if (isInt(arg(t, i, ""))) data = Integer.parseInt(t.get(i++));
                else if (arg(t, i, "").startsWith("[")) i++;
                String java = BedrockNames.block(block, data);
                if (java == null) return null;
                String mode = arg(t, i, "");
                return join("setblock", t.get(1), t.get(2), t.get(3), java, mode.matches("replace|destroy|keep") ? mode : "");
            }
            case "fill": {
                // fill from to block [data] [replace|destroy|hollow|keep|outline] [replaceBlock] [replaceData]
                int i = 7;
                String block = arg(t, i++, "air");
                int data = 0;
                if (isInt(arg(t, i, ""))) data = Integer.parseInt(t.get(i++));
                else if (arg(t, i, "").startsWith("[")) i++;
                String java = BedrockNames.block(block, data);
                if (java == null) return null;
                String mode = arg(t, i++, "replace");
                String coords = String.join(" ", t.subList(1, 7));
                if (mode.equals("replace") && i < t.size()) {
                    String old = t.get(i++);
                    int oldData = isInt(arg(t, i, "")) ? Integer.parseInt(t.get(i)) : -1;
                    String oldJava = BedrockNames.block(old, Math.max(0, oldData));
                    if (oldJava == null) return null;
                    return join("fill", coords, java, "replace", stripProps(oldJava));
                }
                return join("fill", coords, java, mode.matches("replace|destroy|hollow|keep|outline") ? mode : "replace");
            }
            case "clone": {
                // clone begin end destination [replace|masked|filtered] [normal|force|move] [filterBlock] [filterData]
                String coords = String.join(" ", t.subList(1, 10));
                String mask = arg(t, 10, "replace");
                String mode = arg(t, 11, "normal");
                if (mask.equals("filtered")) {
                    String filter = BedrockNames.block(arg(t, 12, "air"), isInt(arg(t, 13, "")) ? Integer.parseInt(t.get(13)) : 0);
                    if (filter == null) return null;
                    return join("clone", coords, "filtered", stripProps(filter), mode);
                }
                return join("clone", coords, mask, mode);
            }
            case "summon": {
                String entity = BedrockNames.entity(arg(t, 1, ""));
                if (entity == null) return null;
                List<String> rest = t.size() >= 5 ? t.subList(2, 5) : List.of("~", "~", "~");
                return join("summon", entity, String.join(" ", rest));
            }
            case "give": {
                // give target item [amount] [data]
                String item = BedrockNames.item(arg(t, 2, ""), isInt(arg(t, 4, "")) ? Integer.parseInt(t.get(4)) : 0);
                if (item == null) return null;
                int amount = isInt(arg(t, 3, "")) ? Integer.parseInt(t.get(3)) : 1;
                return join("give", selector(player, t.get(1)), item, String.valueOf(Math.max(1, amount)));
            }
            case "kill":
                return t.size() > 1 ? join("kill", selector(player, t.get(1))) : "kill @s";
            case "effect": {
                String target = selector(player, arg(t, 1, "@s"));
                String effect = arg(t, 2, "clear");
                if (effect.equals("clear")) return join("effect clear", target);
                effect = EFFECTS.getOrDefault(effect, effect);
                String seconds = arg(t, 3, "30");
                String amp = arg(t, 4, "0");
                String hide = arg(t, 5, "false");
                return join("effect give", target, effect, seconds.equals("infinite") ? "infinite" : seconds, amp, hide);
            }
            case "enchant": {
                String spell = ENCHANTMENTS.getOrDefault(arg(t, 2, ""), arg(t, 2, ""));
                return join("enchant", selector(player, arg(t, 1, "@s")), spell, arg(t, 3, "1"));
            }
            case "tp", "teleport": {
                List<String> out = new ArrayList<>();
                out.add("tp");
                for (int i = 1; i < t.size(); i++) {
                    String a = t.get(i);
                    if (a.equals("facing") || a.equals("true") || a.equals("false")) break;
                    out.add(a.startsWith("@") || !isCoordinate(a) ? selector(player, a) : a);
                }
                return String.join(" ", out);
            }
            case "execute":
                return execute(player, line, t);
            case "say":
                return "say " + rest(line, 1);
            case "tell", "w", "msg", "whisper": {
                String target = selector(player, arg(t, 1, "@s"));
                return "tellraw " + target + " " + quote(rest(line, 2));
            }
            case "title": {
                String target = selector(player, arg(t, 1, "@s"));
                String kind = arg(t, 2, "title");
                if (kind.equals("clear") || kind.equals("reset")) return join("title", target, kind);
                if (kind.equals("times")) return join("title", target, "times", arg(t, 3, "10"), arg(t, 4, "70"), arg(t, 5, "20"));
                return join("title", target, kind, quote(rest(line, 3)));
            }
            case "gamemode": {
                String mode = gameMode(arg(t, 1, "s"));
                return join("gamemode", mode, t.size() > 2 ? selector(player, t.get(2)) : "@s");
            }
            case "difficulty":
                return join("difficulty", difficulty(arg(t, 1, "normal")));
            case "gamerule": {
                String rule = GAME_RULES.get(arg(t, 1, "").toLowerCase(Locale.ROOT));
                return rule == null ? null : join("gamerule", rule, arg(t, 2, ""));
            }
            case "xp": {
                String amount = arg(t, 1, "0");
                String target = t.size() > 2 ? selector(player, t.get(2)) : "@s";
                boolean levels = amount.toUpperCase(Locale.ROOT).endsWith("L");
                if (levels) amount = amount.substring(0, amount.length() - 1);
                return join("xp add", target, amount, levels ? "levels" : "points");
            }
            case "spawnpoint":
                return join("spawnpoint", t.size() > 1 ? selector(player, t.get(1)) : "@s", t.size() > 4 ? String.join(" ", t.subList(2, 5)) : "");
            case "clear":
                if (t.size() > 2) {
                    String item = BedrockNames.item(t.get(2), 0);
                    return join("clear", selector(player, t.get(1)), item != null ? item : "", t.size() > 4 ? t.get(4) : "");
                }
                return join("clear", t.size() > 1 ? selector(player, t.get(1)) : "@s");
            default:
                // other commands are mostly written the same way in Java: just translate the selectors
                StringBuilder sb = new StringBuilder();
                for (String a : t) {
                    if (!sb.isEmpty()) sb.append(' ');
                    sb.append(a.startsWith("@") ? selector(player, a) : a);
                }
                return sb.toString();
        }
    }

    /** Bedrock's old execute (target position command, optionally "detect" a block first) in Java's form */
    private static @Nullable String execute(ServerPlayer player, String line, List<String> t) {
        String second = arg(t, 1, "");
        if (second.matches("as|at|positioned|if|unless|align|anchored|facing|in|run|rotated|store|summon|on")) {
            StringBuilder sb = new StringBuilder("execute");
            for (int i = 1; i < t.size(); i++) sb.append(' ').append(t.get(i).startsWith("@") ? selector(player, t.get(i)) : t.get(i));
            return sb.toString();
        }
        String target = selector(player, second);
        String pos = String.join(" ", t.subList(2, Math.min(5, t.size())));
        int next = 5;
        String condition = "";
        if (arg(t, 5, "").equals("detect")) {
            String detectPos = String.join(" ", t.subList(6, 9));
            String block = BedrockNames.block(arg(t, 9, "air"), isInt(arg(t, 10, "")) ? Integer.parseInt(t.get(10)) : 0);
            if (block == null) return null;
            condition = " if block " + detectPos + " " + stripProps(block);
            next = isInt(arg(t, 10, "")) || arg(t, 10, "").equals("-1") ? 11 : 10;
        }
        String inner = rest(line, next);
        List<String> innerTokens = tokens(inner);
        if (innerTokens.isEmpty()) return null;
        String innerJava = translate(player, inner, innerTokens);
        if (innerJava == null) return null;
        return "execute as " + target + " at @s positioned " + pos + condition + " run " + innerJava;
    }

    /** Bedrock selectors (@s @p @r @a @e, @c for your Agent) and their rules in Java's form */
    static String selector(ServerPlayer player, String s) {
        if (!s.startsWith("@") || s.length() < 2) return s;
        char kind = s.charAt(1);
        if (kind == 'c') {
            AgentEntity agent = Agents.find(player);
            return agent != null ? agent.getUUID().toString() : "@e[type=edu:agent,limit=0]";
        }
        if (kind == 'v') return "@e[type=edu:agent]";
        if (s.startsWith("@initiator")) return "@s";
        String rules = "";
        int open = s.indexOf('[');
        if (open >= 0 && s.endsWith("]")) rules = s.substring(open + 1, s.length() - 1);
        List<String> out = new ArrayList<>();
        String r = null;
        String rm = null;
        String l = null;
        String lm = null;
        String rx = null;
        String rxm = null;
        String ry = null;
        String rym = null;
        for (String pair : splitTop(rules, ',')) {
            int eq = pair.indexOf('=');
            if (eq < 0) continue;
            String key = pair.substring(0, eq).trim();
            String value = pair.substring(eq + 1).trim();
            switch (key) {
                case "r" -> r = value;
                case "rm" -> rm = value;
                case "l" -> l = value;
                case "lm" -> lm = value;
                case "rx" -> rx = value;
                case "rxm" -> rxm = value;
                case "ry" -> ry = value;
                case "rym" -> rym = value;
                case "c" -> {
                    int c = isInt(value) ? Integer.parseInt(value) : 1;
                    out.add("limit=" + Math.max(1, Math.abs(c)));
                    if (kind != 'p' && kind != 'r') out.add(c < 0 ? "sort=furthest" : "sort=nearest");
                }
                case "m" -> {
                    boolean not = value.startsWith("!");
                    out.add("gamemode=" + (not ? "!" : "") + gameMode(not ? value.substring(1) : value));
                }
                case "type" -> {
                    boolean not = value.startsWith("!");
                    String java = BedrockNames.entity(not ? value.substring(1) : value);
                    if (java != null) out.add("type=" + (not ? "!" : "") + java);
                }
                case "family", "hasitem", "haspermission", "has_property" -> {
                }
                default -> out.add(key + "=" + value);
            }
        }
        if (r != null || rm != null) out.add("distance=" + (rm != null ? rm : "") + ".." + (r != null ? r : ""));
        if (l != null || lm != null) out.add("level=" + (lm != null ? lm : "") + ".." + (l != null ? l : ""));
        if (rx != null || rxm != null) out.add("x_rotation=" + (rxm != null ? rxm : "") + ".." + (rx != null ? rx : ""));
        if (ry != null || rym != null) out.add("y_rotation=" + (rym != null ? rym : "") + ".." + (ry != null ? ry : ""));
        char javaKind = "sprae".indexOf(kind) >= 0 ? kind : 'e';
        return "@" + javaKind + (out.isEmpty() ? "" : "[" + String.join(",", out) + "]");
    }

    private static String gameMode(String mode) {
        return switch (mode.toLowerCase(Locale.ROOT)) {
            case "1", "c", "creative" -> "creative";
            case "2", "a", "adventure" -> "adventure";
            case "3", "6", "sp", "spectator" -> "spectator";
            default -> "survival";
        };
    }

    private static String difficulty(String d) {
        return switch (d.toLowerCase(Locale.ROOT)) {
            case "0", "p", "peaceful" -> "peaceful";
            case "1", "e", "easy" -> "easy";
            case "3", "h", "hard" -> "hard";
            default -> "normal";
        };
    }

    // ---------------------------------------------------------------- commands answered here

    private static Result agent(ServerPlayer player, List<String> args) throws Exception {
        String sub = arg(args, 0, "").toLowerCase(Locale.ROOT);
        ServerLevel level = player.serverLevel();
        switch (sub) {
            case "create":
            case "tp": {
                Vec3 at = Vec3.atBottomCenterOf(player.blockPosition());
                Direction facing = player.getDirection();
                if (args.size() >= 4) {
                    CommandSourceStack source = player.createCommandSourceStack();
                    at = Vec3.atBottomCenterOf(BlockPosArgument.blockPos().parse(new StringReader(String.join(" ", args.subList(1, 4))))
                        .getBlockPos(source));
                    if (args.size() >= 5) facing = Direction.fromYRot(Float.parseFloat(args.get(4)));
                }
                if (level.isOutsideBuildHeight(BlockPos.containing(at)) || !level.getWorldBorder().isWithinBounds(BlockPos.containing(at))) {
                    return Result.fail("Agent failed to teleport");
                }
                return Agents.summon(player, level, at, facing) ? Result.ok(sub.equals("tp") ? "Agent teleported" : "Created Agent")
                    : Result.fail("Unable to create Agent");
            }
            case "getposition": {
                AgentEntity agent = AgentActions.agentFor(player);
                if (agent == null) return Result.fail("Cannot issue command, Agent is out of range");
                BlockPos pos = agent.blockPosition();
                return Result.ok("Agent getposition successful").with("position", position(pos))
                    .with("y-rot", (double) net.minecraft.util.Mth.wrapDegrees(agent.getYRot()));
            }
            case "setitem": {
                // setitem slot item count [data]
                AgentEntity agent = AgentActions.agentFor(player);
                if (agent == null) return Result.fail("Cannot issue command, Agent is out of range");
                int slot = Integer.parseInt(arg(args, 1, "1")) - 1;
                int data = isInt(arg(args, 4, "")) ? Integer.parseInt(args.get(4)) : 0;
                String item = BedrockNames.item(arg(args, 2, "air"), data);
                int count = Integer.parseInt(arg(args, 3, "1"));
                if (item == null || slot < 0 || slot >= AgentEntity.SLOTS) return Result.fail(INVALID_CALL, "Agent failed to set item");
                ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(item)),
                    Math.max(0, count));
                if (stack.getCount() > stack.getMaxStackSize()) stack.setCount(stack.getMaxStackSize());
                agent.inventory().setItem(slot, stack);
                return Result.ok("Agent set item successful");
            }
            default:
                // the Agent's actions normally come as "action:agent" requests; answer them right away if not
                return AgentActions.runNow(player, args);
        }
    }

    private static Result testForBlock(ServerPlayer player, List<String> t) throws Exception {
        BlockPos pos = blockPos(player, t, 1);
        int data = isInt(arg(t, 5, "")) ? Integer.parseInt(t.get(5)) : 0;
        String wanted = BedrockNames.block(arg(t, 4, "air"), Math.max(0, data));
        if (wanted == null) return Result.fail(INVALID_CALL, "Unknown block");
        BlockState state = player.serverLevel().getBlockState(pos);
        String actual = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        String wantedId = stripProps(wanted);
        if (wantedId.indexOf(':') < 0) wantedId = "minecraft:" + wantedId;
        boolean matches = actual.equals(wantedId);
        String at = pos.getX() + "," + pos.getY() + "," + pos.getZ();
        String message = matches ? "Successfully found the block at " + at + "."
            : "The block at " + at + " is " + BedrockNames.bedrock(state).id() + " (expected: " + arg(t, 4, "air") + ").";
        return Result.ok(message).with("matches", matches).with("position", position(pos));
    }

    private static Result testForBlocks(ServerPlayer player, List<String> t) {
        String mode = arg(t, 10, "all");
        Result r = java(player, "execute if blocks " + String.join(" ", t.subList(1, 10)) + " " + (mode.equals("masked") ? "masked" : "all"));
        boolean matches = r.status() == OK;
        return Result.ok(matches ? "Blocks match" : "Blocks don't match").with("matches", matches);
    }

    private static Result topSolidBlock(ServerPlayer player, List<String> t) throws Exception {
        BlockPos start = blockPos(player, t, 1);
        ServerLevel level = player.serverLevel();
        int x = start.getX();
        int z = start.getZ();
        int y = Math.min(start.getY(), level.getMaxBuildHeight() - 1);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(x, y, z);
        if (solid(level, p)) {
            while (p.getY() < level.getMaxBuildHeight() - 1 && solid(level, p.above())) p.move(Direction.UP);
        } else {
            while (p.getY() > level.getMinBuildHeight() && !solid(level, p)) p.move(Direction.DOWN);
            if (!solid(level, p)) return Result.fail("No solid block below " + x + "," + y + "," + z);
        }
        return Result.ok("Top solid block found").with("position", position(p.immutable()));
    }

    private static boolean solid(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.getCollisionShape(level, pos).isEmpty() && !state.liquid();
    }

    private static Result queryTarget(ServerPlayer player, List<String> t) throws Exception {
        String target = selector(player, arg(t, 1, "@s"));
        CommandSourceStack source = source(player, null);
        List<? extends Entity> entities = EntityArgument.entities().parse(new StringReader(target)).findEntities(source);
        JsonArray details = new JsonArray();
        for (Entity e : entities) {
            JsonObject o = new JsonObject();
            String dim = e.level().dimension().location().getPath();
            o.addProperty("dimension", dim.equals("the_nether") ? 1 : dim.equals("the_end") ? 2 : 0);
            JsonObject pos = new JsonObject();
            pos.addProperty("x", e.getX());
            // Bedrock gives a player's position at eye level
            pos.addProperty("y", e instanceof Player ? e.getEyeY() : e.getY());
            pos.addProperty("z", e.getZ());
            o.add("position", pos);
            o.addProperty("uniqueId", e.getUUID().toString());
            o.addProperty("yRot", net.minecraft.util.Mth.wrapDegrees(e.getYRot()));
            details.add(o);
        }
        if (details.isEmpty()) return Result.fail("No targets matched selector");
        return Result.ok("Target data").with("details", details.toString());
    }

    private static Result time(ServerPlayer player, List<String> t) {
        String sub = arg(t, 1, "");
        if (sub.equals("query")) {
            String what = arg(t, 2, "daytime");
            Result r = java(player, "time query " + (what.matches("daytime|gametime|day") ? what : "daytime"));
            int value = r.extra().has("value") ? r.extra().get("value").getAsInt() : 0;
            String label = what.equals("gametime") ? "Gametime" : what.equals("day") ? "Day" : "Daytime";
            return new Result(r.status(), label + " is " + value, r.extra());
        }
        String value = arg(t, 2, "0");
        value = switch (value) {
            case "sunrise" -> "23000";
            case "sunset" -> "12000";
            default -> value;
        };
        return java(player, join("time", sub, value));
    }

    private static Result weather(ServerPlayer player, List<String> t) {
        String sub = arg(t, 1, "clear");
        if (sub.equals("query")) {
            Level level = player.serverLevel();
            int w = level.isThundering() ? 2 : level.isRaining() ? 1 : 0;
            return Result.ok("Weather state is: " + new String[] {"clear", "rain", "thunder"}[w]).with("data", w);
        }
        return java(player, join("weather", sub, arg(t, 2, "")));
    }

    // ---------------------------------------------------------------- running a Java command

    /** the command source a MakeCode command runs with (the player, with operator commands for the host and World Builders) */
    static CommandSourceStack source(ServerPlayer player, @Nullable CommandSource output) {
        CommandSourceStack source = player.createCommandSourceStack();
        if (output != null) source = source.withSource(output);
        if (EduPermissions.isHost(player) || EduPermissions.isWorldBuilder(player)) source = source.withPermission(2);
        return source;
    }

    /** runs a Java command as the player and turns the outcome into a Bedrock answer */
    static Result java(ServerPlayer player, String command) {
        if (EduPermissions.isWorldBuilderCommand(command) && !EduPermissions.isHost(player)) {
            return Result.fail("Only the world's host can use that command");
        }
        StringBuilder out = new StringBuilder();
        boolean[] success = {false};
        int[] value = {0};
        CommandSource capture = new CommandSource() {
            @Override
            public void sendSystemMessage(Component message) {
                if (!out.isEmpty()) out.append('\n');
                out.append(message.getString());
            }

            @Override
            public boolean acceptsSuccess() {
                return true;
            }

            @Override
            public boolean acceptsFailure() {
                return true;
            }

            @Override
            public boolean shouldInformAdmins() {
                return false;
            }
        };
        CommandSourceStack source = source(player, capture).withCallback((ok, result) -> {
            success[0] |= ok;
            value[0] = result;
        });
        player.getServer().getCommands().performPrefixedCommand(source, command);
        String message = out.isEmpty() ? (success[0] ? "Done" : "Command failed") : out.toString();
        Result r = success[0] ? Result.ok(message) : Result.fail(message);
        return r.with("value", value[0]);
    }

    // ---------------------------------------------------------------- helpers

    static String playerName(ServerPlayer player) {
        return player.getGameProfile().getName();
    }

    static JsonObject position(BlockPos pos) {
        JsonObject o = new JsonObject();
        o.addProperty("x", pos.getX());
        o.addProperty("y", pos.getY());
        o.addProperty("z", pos.getZ());
        return o;
    }

    private static BlockPos blockPos(ServerPlayer player, List<String> t, int from) throws Exception {
        String coords = arg(t, from, "~") + " " + arg(t, from + 1, "~") + " " + arg(t, from + 2, "~");
        return BlockPosArgument.blockPos().parse(new StringReader(coords)).getBlockPos(player.createCommandSourceStack());
    }

    static Vec3 vec(ServerPlayer player, String coords) throws Exception {
        return Vec3Argument.vec3().parse(new StringReader(coords)).getPosition(player.createCommandSourceStack());
    }

    /** splits a command line at spaces, keeping [...], {...} and "..." together */
    static List<String> tokens(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int depth = 0;
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"' && (i == 0 || line.charAt(i - 1) != '\\')) quoted = !quoted;
            else if (!quoted && (c == '[' || c == '{')) depth++;
            else if (!quoted && (c == ']' || c == '}')) depth = Math.max(0, depth - 1);
            if (c == ' ' && !quoted && depth == 0) {
                if (!cur.isEmpty()) {
                    out.add(cur.toString());
                    cur.setLength(0);
                }
            } else {
                cur.append(c);
            }
        }
        if (!cur.isEmpty()) out.add(cur.toString());
        return out;
    }

    /** the text of the line after its first {@code n} tokens (for messages, which keep their spacing) */
    static String rest(String line, int n) {
        List<String> t = tokens(line);
        if (n >= t.size()) return "";
        int pos = 0;
        for (int i = 0; i < n; i++) {
            pos = line.indexOf(t.get(i), pos) + t.get(i).length();
        }
        return line.substring(pos).trim();
    }

    private static List<String> splitTop(String s, char sep) {
        List<String> out = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' || c == '[') depth++;
            else if (c == '}' || c == ']') depth--;
            else if (c == sep && depth == 0) {
                out.add(s.substring(start, i));
                start = i + 1;
            }
        }
        if (start < s.length()) out.add(s.substring(start));
        return out;
    }

    static String arg(List<String> t, int i, String fallback) {
        return i < t.size() ? t.get(i) : fallback;
    }

    static boolean isInt(String s) {
        return s.matches("-?\\d+");
    }

    private static boolean isCoordinate(String s) {
        return s.matches("[~^]?-?\\d*\\.?\\d*");
    }

    private static String bool(String s) {
        return s.equalsIgnoreCase("false") || s.equals("0") ? "false" : "true";
    }

    private static String stripProps(String id) {
        int bracket = id.indexOf('[');
        return bracket >= 0 ? id.substring(0, bracket) : id;
    }

    /** a JSON string, which Java's text arguments accept as plain text */
    static String quote(String text) {
        return "\"" + text.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String join(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p == null || p.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(p);
        }
        return sb.toString();
    }

    private BedrockCommands() {
    }
}
