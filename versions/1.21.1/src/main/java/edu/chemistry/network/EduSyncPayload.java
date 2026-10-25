package edu.chemistry.network;

import edu.chemistry.EduMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to client: one of the mod's values ("education_features", "immutable_world", "world_builder"). Minecraft 1.21.1
 * has no packet for game rule values, so this small packet does that job; CustomPayloadCodecMixin teaches the game's
 * custom packet codec about it.
 */
public record EduSyncPayload(String name, boolean value) implements CustomPacketPayload {
    public static final Type<EduSyncPayload> TYPE = new Type<>(EduMod.id("sync"));
    public static final StreamCodec<ByteBuf, EduSyncPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, EduSyncPayload::name, ByteBufCodecs.BOOL, EduSyncPayload::value, EduSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
