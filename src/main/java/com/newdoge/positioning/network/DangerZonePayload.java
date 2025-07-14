package com.newdoge.positioning.network;

import com.newdoge.positioning.Positioning;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.util.Identifier;

public record DangerZonePayload(int secondsLeft) implements CustomPayload {
    public static final CustomPayload.Id<DangerZonePayload> ID =
            new Id<>(Identifier.of(Positioning.MOD_ID, "danger_zone_timer"));

    public static final PacketCodec<RegistryByteBuf, DangerZonePayload> CODEC =
            PacketCodec.of(
                    (payload, buf) -> buf.writeVarInt(payload.secondsLeft()),
                    buf -> new DangerZonePayload(buf.readVarInt())
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
