package edu.chemistry.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import edu.chemistry.EduPermissions;
import edu.chemistry.EduRules;
import java.util.Collection;
import java.util.List;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Education's /wb (also /worldbuilder): gives or takes World Builder.
 * <ul>
 * <li>/wb - toggles it for yourself</li>
 * <li>/wb &lt;players&gt; - toggles it for those players</li>
 * <li>/wb &lt;players&gt; true|false - turns it on or off for them</li>
 * </ul>
 * Players: only the world's host (the player who owns the singleplayer / LAN world), or owner-level operators on a
 * dedicated server. It also works in command blocks, command block minecarts, functions and the server console.
 */
@Mixin(Commands.class)
public abstract class CommandsMixin {
    @Shadow @Final private CommandDispatcher<CommandSourceStack> dispatcher;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void edu$registerWorldBuilder(Commands.CommandSelection selection, CommandBuildContext context, CallbackInfo ci) {
        for (String name : new String[] {"wb", "worldbuilder"}) {
            this.dispatcher.register(Commands.literal(name)
                .requires(CommandsMixin::edu$isHost)
                .executes(ctx -> edu$apply(ctx, List.of(ctx.getSource().getPlayerOrException()), null))
                .then(Commands.argument("targets", EntityArgument.players())
                    .executes(ctx -> edu$apply(ctx, EntityArgument.getPlayers(ctx, "targets"), null))
                    .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(ctx -> edu$apply(ctx, EntityArgument.getPlayers(ctx, "targets"), BoolArgumentType.getBool(ctx, "value"))))));
        }
    }

    @Unique
    private static boolean edu$isHost(CommandSourceStack source) {
        // the stand-in source the game uses to see which commands are op-only (it has no server or player)
        if (source.getServer() == null) return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
        // who really ran it: a player (even through /execute as), or the console / a command block / a function
        ServerPlayer player = EduPermissions.typedBy(source);
        if (player != null) return EduPermissions.isHost(player);
        return source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    /** players who joined the world (anyone but the host) can't run World Builder commands, even through /execute */
    @Inject(method = "performCommand", at = @At("HEAD"), cancellable = true)
    private void edu$hostOnly(com.mojang.brigadier.ParseResults<CommandSourceStack> parse, String command, CallbackInfo ci) {
        CommandSourceStack source = parse.getContext().getSource();
        if (!EduPermissions.isWorldBuilderCommand(command)) return;
        ServerPlayer player = EduPermissions.typedBy(source);
        if (player != null && !EduPermissions.isHost(player)) {
            EduPermissions.tellHostOnly(player);
            ci.cancel();
        }
    }

    @Unique
    private static int edu$apply(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players, Boolean value) {
        for (ServerPlayer player : players) {
            boolean on = value != null ? value : !player.entityTags().contains(EduPermissions.WORLD_BUILDER_TAG);
            if (on) player.addTag(EduPermissions.WORLD_BUILDER_TAG);
            else player.removeTag(EduPermissions.WORLD_BUILDER_TAG);
            player.connection.send(EduRules.syncPacket("world_builder", on));
            Component who = player.getDisplayName();
            ctx.getSource().sendSuccess(() -> Component.translatable(on ? "commands.edu.wb.on" : "commands.edu.wb.off", who), true);
            if (ctx.getSource().getEntity() != player) {
                player.sendSystemMessage(Component.translatable(on ? "commands.edu.wb.you_on" : "commands.edu.wb.you_off"));
            }
        }
        return players.size();
    }
}
