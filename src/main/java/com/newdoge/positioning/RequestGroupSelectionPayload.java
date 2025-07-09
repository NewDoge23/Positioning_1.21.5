package com.newdoge.positioning;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record RequestGroupSelectionPayload() implements CustomPayload {
    public static final CustomPayload.Id<RequestGroupSelectionPayload> ID =
            new Id<>(Identifier.of(Positioning.MOD_ID, "request_group_selection"));
    public static final PacketCodec<RegistryByteBuf, RequestGroupSelectionPayload> CODEC =
            PacketCodec.unit(new RequestGroupSelectionPayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
