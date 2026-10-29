package edu.chemistry.network;

import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The mod's own packets (Code Builder's): how they're sent, and where they go on the client. Fabric and NeoForge send
 * them as the game's custom payload packets; Forge sends them through its network channel, so its entry point swaps
 * {@link #toClient} and {@link #toServer}. Each loader hands arriving packets to the server or the client's handler.
 */
public final class EduNetwork {
    /** the packet that takes one of the mod's payloads to a client */
    public static Function<CustomPacketPayload, Packet<?>> toClient = ClientboundCustomPayloadPacket::new;
    /** the packet that takes one of the mod's payloads to the server */
    public static Function<CustomPacketPayload, Packet<?>> toServer = ServerboundCustomPayloadPacket::new;
    /** the client's handler for the mod's payloads (on the client thread), set by the client's setup */
    public static Consumer<CustomPacketPayload> clientHandler = payload -> {
    };

    private EduNetwork() {
    }
}
