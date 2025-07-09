package com.newdoge.positioning;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ModMethods implements ServerTickEvents.EndTick {

    private final Map<UUID, Long> playerCheckCooldowns = new HashMap<>();
    private final Map<UUID, Boolean> playerWarned = new HashMap<>();
    private final Map<UUID, Long> dangerZoneEnteredAt = new HashMap<>();
    private final Map<UUID, Boolean> wasInNeutralZone = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(new ModMethods());
    }

    @Override
    public void onEndTick(MinecraftServer server) {
        long currentTick = server.getTicks();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID playerId = player.getUuid();

            // 1. ¿Eligió grupo? Si no, pedir selección y saltar este tick
            if (!PlayerGroupManager.hasGroup(playerId)) {
                ServerPlayNetworking.send(player, new RequestGroupSelectionPayload());
                sendDangerZonePacket(player, 0); // Oculta barra si estaba activa
                continue;
            }

            if (!player.getWorld().getRegistryKey().equals(net.minecraft.world.World.OVERWORLD)) {
                // Si no está en el Overworld, NO hacer nada especial, salteá este jugador
                sendDangerZonePacket(player, 0);
                continue;
            }


            double z = player.getZ();
            Integer group = PlayerGroupManager.getGroup(playerId);

            // 2. Cooldown por jugador (0,5s)
            long lastCheck = playerCheckCooldowns.getOrDefault(playerId, 0L);
            if (currentTick - lastCheck < 10) continue;
            playerCheckCooldowns.put(playerId, currentTick);

            // Lógica especial según grupo
            if (group == 1) { // Grupo NORTE (Z+)
                if (z > 50) {
                    // Zona segura
                    if (wasInNeutralZone.getOrDefault(playerId, false)) {
                        player.sendMessage(Text.literal("¡Volviste a zona segura!"), false);
                        wasInNeutralZone.put(playerId, false);
                    }
                    resetPlayerState(playerId, player);
                    continue;
                }
                if (z <= 50 && z >= 0) {
                    // Zona neutral, advertencia única
                    wasInNeutralZone.put(playerId, true);
                    if (!playerWarned.getOrDefault(playerId, false)) {
                        player.sendMessage(Text.literal("¡Zona neutral! Estás cerca del límite sur."), false);
                        playerWarned.put(playerId, true);
                    }
                    sendDangerZonePacket(player, 0);
                    dangerZoneEnteredAt.remove(playerId);
                    continue;
                }
                if (z < 0 && z > -50) {
                    // Zona prohibida, cuenta regresiva
                    handleDangerZoneCountdown(server, player, playerId, currentTick, true);
                    continue;
                }
                if (z <= -50) {
                    // Muerte instantánea
                    killInstantly(player, playerId, "por cruzar el límite sur (-50) siendo grupo norte");
                    continue;
                }
            } else if (group == 2) { // Grupo SUR (Z-)
                if (z < -50) {
                    // Zona segura
                    if (wasInNeutralZone.getOrDefault(playerId, false)) {
                        player.sendMessage(Text.literal("¡Volviste a zona segura!"), false);
                        wasInNeutralZone.put(playerId, false);
                    }
                    resetPlayerState(playerId, player);
                    continue;
                }
                if (z >= -50 && z <= 0) {
                    // Zona neutral, advertencia única
                    wasInNeutralZone.put(playerId, true);
                    if (!playerWarned.getOrDefault(playerId, false)) {
                        player.sendMessage(Text.literal("¡Zona neutral! Estás cerca del límite norte."), false);
                        playerWarned.put(playerId, true);
                    }
                    sendDangerZonePacket(player, 0);
                    dangerZoneEnteredAt.remove(playerId);
                    continue;
                }
                if (z > 0 && z < 50) {
                    // Zona prohibida, cuenta regresiva
                    handleDangerZoneCountdown(server, player, playerId, currentTick, false);
                    continue;
                }
                if (z >= 50) {
                    // Muerte instantánea
                    killInstantly(player, playerId, "por cruzar el límite norte (+50) siendo grupo sur");
                    continue;
                }
            }
        }
    }

    // Resetea estados si está en zona segura
    private void resetPlayerState(UUID playerId, ServerPlayerEntity player) {
        playerWarned.put(playerId, false);
        dangerZoneEnteredAt.remove(playerId);
        sendDangerZonePacket(player, 0);
    }

    // Lógica de cuenta regresiva
    private void handleDangerZoneCountdown(MinecraftServer server, ServerPlayerEntity player, UUID playerId, long currentTick, boolean esNorte) {
        if (!dangerZoneEnteredAt.containsKey(playerId)) {
            dangerZoneEnteredAt.put(playerId, currentTick);
            String msg = esNorte ?
                    "¡PELIGRO! Estás en la zona prohibida sur. Tenés 60 segundos para volver a tu lado (+Z) o morís."
                    :
                    "¡PELIGRO! Estás en la zona prohibida norte. Tenés 60 segundos para volver a tu lado (-Z) o morís.";
            player.sendMessage(Text.literal(msg), false);
        }
        long enteredAt = dangerZoneEnteredAt.get(playerId);
        int secondsLeft = (int) (60 - (currentTick - enteredAt) / 20);

        if (secondsLeft > 0) {
            sendDangerZonePacket(player, secondsLeft);
        } else {
            player.sendMessage(Text.literal("Moriste por invadir demasiado la zona enemiga."), false);
            Positioning.LOGGER.info("Matando a: {} por quedarse en zona prohibida", player.getName().getString());
            player.setHealth(0.0F);
            dangerZoneEnteredAt.remove(playerId);
            playerWarned.put(playerId, false);
            sendDangerZonePacket(player, 0);
        }
    }

    // Muerte instantánea
    private void killInstantly(ServerPlayerEntity player, UUID playerId, String reason) {
        player.sendMessage(Text.literal("¡Moriste por cruzar el límite final!"), false);
        Positioning.LOGGER.info("Matando a: {} {}", player.getName().getString(), reason);
        player.setHealth(0.0F);
        dangerZoneEnteredAt.remove(playerId);
        playerWarned.put(playerId, false);
        sendDangerZonePacket(player, 0);
    }

    private void sendDangerZonePacket(ServerPlayerEntity player, int secondsLeft) {
        ServerPlayNetworking.send(player, new DangerZonePayload(secondsLeft));
    }
}
