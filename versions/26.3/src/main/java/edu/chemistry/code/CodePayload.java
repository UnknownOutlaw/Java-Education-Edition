package edu.chemistry.code;

import edu.chemistry.EduMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Code Builder messages between a player's game and the world's server, both ways: MakeCode's packets (JSON) wrapped
 * with the editor tab they belong to. CustomPayloadCodecMixin teaches the game's custom packet codec about it.
 */
public record CodePayload(String json) implements CustomPacketPayload {
    public static final Type<CodePayload> TYPE = new Type<>(EduMod.id("code"));
    public static final StreamCodec<ByteBuf, CodePayload> CODEC = ByteBufCodecs.stringUtf8(1 << 20).map(CodePayload::new, CodePayload::json);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
