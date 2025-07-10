package com.newdoge.positioning;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record SubmitGroupSelectionPayload(int group) implements CustomPayload {
    public static final CustomPayload.Id<SubmitGroupSelectionPayload> ID =
            new Id<>(Identifier.of(Positioning.MOD_ID, "submit_group_selection"));
    public static final PacketCodec<RegistryByteBuf, SubmitGroupSelectionPayload> CODEC =
            PacketCodec.of(
                    (payload, buf) -> buf.writeVarInt(payload.group()),
                    buf -> new SubmitGroupSelectionPayload(buf.readVarInt())
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
