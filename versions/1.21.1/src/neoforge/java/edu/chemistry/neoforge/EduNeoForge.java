package edu.chemistry.neoforge;

import edu.chemistry.EduMod;
import edu.chemistry.code.CodeConnection;
import edu.chemistry.code.CodePayload;
import edu.chemistry.network.EduNetwork;
import edu.chemistry.network.EduSyncPayload;
import edu.chemistry.registry.EduRegistration;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * NeoForge's entry point. The registrations run in NeoForge's registration event for each registry, and the mod's
 * packets (the rule sync and Code Builder's) are registered with NeoForge's networking (they then travel as the
 * game's custom payload packets, as on Fabric).
 */
@Mod(EduMod.MOD_ID)
public final class EduNeoForge {
    public EduNeoForge(IEventBus modBus, Dist dist) {
        EduMod.loader = EduMod.Loader.NEOFORGE;
        modBus.addListener((RegisterEvent event) -> EduRegistration.register(event.getRegistryKey()));
        modBus.addListener((FMLCommonSetupEvent event) -> EduRegistration.checkDone());
        modBus.addListener(EduNeoForge::registerPayloads);
        if (dist == Dist.CLIENT) EduNeoForgeClient.init(modBus);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1").executesOn(HandlerThread.MAIN);
        registrar.playToClient(EduSyncPayload.TYPE, EduSyncPayload.CODEC, (payload, context) -> EduNetwork.clientHandler.accept(payload));
        registrar.playBidirectional(CodePayload.TYPE, CodePayload.CODEC, (payload, context) -> {
            if (context.flow() == PacketFlow.CLIENTBOUND) EduNetwork.clientHandler.accept(payload);
            else if (context.player() instanceof ServerPlayer player) CodeConnection.receive(player, payload.json());
        });
    }
}
