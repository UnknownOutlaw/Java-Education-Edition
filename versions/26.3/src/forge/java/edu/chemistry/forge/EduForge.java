package edu.chemistry.forge;

import edu.chemistry.EduMod;
import edu.chemistry.code.CodeConnection;
import edu.chemistry.code.CodePayload;
import edu.chemistry.network.EduNetwork;
import edu.chemistry.registry.EduRegistration;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.bus.BusGroup;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.registries.RegisterEvent;

/**
 * Forge's entry point. The registrations run in Forge's registration event for each registry (Forge only lets a
 * registry be added to during its own event), and Code Builder's packets go through a Forge network channel.
 */
@Mod(EduMod.MOD_ID)
public final class EduForge {
    private static final StreamCodec<RegistryFriendlyByteBuf, CodePayload> CODE_CODEC = StreamCodec.of(CodePayload.CODEC::encode, CodePayload.CODEC::decode);

    public EduForge(FMLJavaModLoadingContext context) {
        EduMod.loader = EduMod.Loader.FORGE;
        BusGroup modBus = context.getModBusGroup();
        RegisterEvent.getBus(modBus).addListener(event -> EduRegistration.register(event.getRegistryKey()));
        FMLCommonSetupEvent.getBus(modBus).addListener(event -> EduRegistration.checkDone());

        Channel<CustomPacketPayload> channel = ChannelBuilder.named(EduMod.id("main")).networkProtocolVersion(1).payloadChannel()
            .play().bidirectional()
            .addMain(CodePayload.TYPE, CODE_CODEC, (payload, context2) -> {
                if (context2.isClientSide()) EduNetwork.clientHandler.accept(payload);
                else if (context2.getSender() != null) CodeConnection.receive(context2.getSender(), payload.json());
            })
            .build();
        EduNetwork.toClient = payload -> NetworkDirection.PLAY_TO_CLIENT.buildPacket(channel, payload);
        EduNetwork.toServer = payload -> NetworkDirection.PLAY_TO_SERVER.buildPacket(channel, payload);

        if (FMLEnvironment.dist == Dist.CLIENT) EduForgeClient.init(modBus);
    }
}
