package com.newdoge.positioning;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Uuids;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public record SyncGroupsPayload(Map<UUID, Integer> groups) implements CustomPayload {
    public static final CustomPayload.Id<SyncGroupsPayload> ID =
            CustomPayload.id("sync_groups");

    public static final PacketCodec<RegistryByteBuf, SyncGroupsPayload> CODEC =
            PacketCodec.of(
                    (payload, buf) -> {
                        buf.writeVarInt(payload.groups.size());
                        payload.groups.forEach((uuid, group) -> {
                            Uuids.PACKET_CODEC.encode(buf, uuid);  // <--- buffer primero!
                            buf.writeVarInt(group);
                        });
                    },
                    buf -> {
                        int size = buf.readVarInt();
                        Map<UUID, Integer> groups = new HashMap<>();
                        for (int i = 0; i < size; i++) {
                            UUID uuid = Uuids.PACKET_CODEC.decode(buf);
                            int group = buf.readVarInt();
                            groups.put(uuid, group);
                        }
                        return new SyncGroupsPayload(groups);
                    }
            );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
