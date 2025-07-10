package com.newdoge.positioning;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.network.packet.s2c.play.SubtitleS2CPacket;

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

            if (player.isDead() || player.isRemoved() || player.getHealth() <= 0.0F) {
                // Limpiá títulos por si quedaron pegados, pero no sigas con lógica de zona peligrosa
                player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("")));
                player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("")));
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
                    // Limpiá el título por si venía de zona prohibida
                    player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("")));
                    player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("")));
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
                    // Limpiá el título por si venía de zona prohibida
                    player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("")));
                    player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("")));
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
        // Limpiá el título si estaba
        player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("")));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("")));
        playerWarned.put(playerId, false);
        dangerZoneEnteredAt.remove(playerId);
        sendDangerZonePacket(player, 0);
    }

    // Lógica de cuenta regresiva
    private void handleDangerZoneCountdown(MinecraftServer server, ServerPlayerEntity player, UUID playerId, long currentTick, boolean esNorte) {
        if (!dangerZoneEnteredAt.containsKey(playerId)) {
            dangerZoneEnteredAt.put(playerId, currentTick);

            // Título rojo con tiempo
            Text title = Text.literal("¡Volvé a tu territorio!").styled(s -> s.withColor(0xFF4444));
            Text subtitle = Text.literal("Tenés 30 segundos para volver").styled(s -> s.withColor(0xFF4444));

            // --- SONIDO DE ALERTA ---
            player.playSound(
                    SoundEvents.BLOCK_BELL_USE,
                    3.0F, // volumen
                    1.0F  // pitch
            );

            player.networkHandler.sendPacket(new TitleS2CPacket(title));
            player.networkHandler.sendPacket(new SubtitleS2CPacket(subtitle));
        }
        long enteredAt = dangerZoneEnteredAt.get(playerId);
        int secondsLeft = (int) (30 - (currentTick - enteredAt) / 20); // Ahora 30 segundos

        if (secondsLeft > 0) {
            sendDangerZonePacket(player, secondsLeft);
        } else {
            // Limpiá el título al morir
            player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("")));
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
        // Limpiá el título
        player.networkHandler.sendPacket(new TitleS2CPacket(Text.literal("")));
        player.networkHandler.sendPacket(new SubtitleS2CPacket(Text.literal("")));
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
