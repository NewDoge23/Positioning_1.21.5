package com.newdoge.positioning;



import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ModMethods implements ServerTickEvents.EndTick {

    private final Map<UUID, Long> playerCheckCooldowns = new HashMap<>(); // Para chequear cada 5 seg
    private final Map<UUID, Boolean> playerWarned= new HashMap<>(); // Si ya se le advirtió por zona cercana
    private final Map<UUID, Long> dangerZoneEnteredAt = new HashMap<>(); // Tick en el que entró en zona peligrosa

    // Registra el evento en el mod
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(new ModMethods());
    }

    @Override
    public void onEndTick(MinecraftServer server) {
        //Positioning.LOGGER.info("onEndTick activo"); //LOG PARA DEBUG

        long currentTick = server.getTicks();

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            UUID playerId = player.getUuid();
            double z = player.getZ();

            // Cooldown general: chequea solo cada 5 segundos (100 ticks)
            long lastCheck = playerCheckCooldowns.getOrDefault(playerId, 0L);
            if (currentTick - lastCheck < 100) continue; // todavía no revisa de nuevo
            playerCheckCooldowns.put(playerId, currentTick);

            // Revisa si está fuera de la zona de peligro
            if (z > 50) {
                // Resetear advertencias y cuenta regresiva
                playerWarned.put(playerId, false);
                dangerZoneEnteredAt.remove(playerId);
                sendDangerZonePacket(player, 0);
                continue;
            }

            // Si está dentro de la zona de peligro
            if (z <= 50 && z > 0) {
                if (!playerWarned.getOrDefault(playerId, false)) {
                    player.sendMessage(Text.literal("Estás en la zona neutral. Fijate donde pisas. Si te da.."), false);
                    playerWarned.put(playerId, true);
                    Positioning.LOGGER.info("Jugador: {} Z: {}", player.getName().getString(), player.getZ()); // LOG a consola
                }
                // Si sale de la zona de peligro = cancela la cuenta regresiva
                dangerZoneEnteredAt.remove(playerId);
                sendDangerZonePacket(player, 0);
                continue;
            }

            // Si cruza el limite
            if (z <= 0) {
                // Apenas cruza el límite = Inicia la cuenta regresiva
                if (!dangerZoneEnteredAt.containsKey(playerId)) {
                    dangerZoneEnteredAt.put(playerId, currentTick);
                    player.sendMessage(Text.literal("¡PELIGRO! Tenés 60 segundos para pegar la vuelta. Primer advertencia"), false);
                }
                long enteredAt = dangerZoneEnteredAt.get(playerId);
                int secondsLeft = (int)(60 - (currentTick - enteredAt) / 20);

                if (secondsLeft > 0) {
                    sendDangerZonePacket(player, secondsLeft);
                    player.sendMessage(Text.literal("Volvé, o morís en: " + secondsLeft + "segundos"), true);
                } else {
                    player.sendMessage(Text.literal("Se te avisó."), false);
                    Positioning.LOGGER.info("Matando a: {} por cruzar la ZN", player.getName().getString()); // LOG a consola
                    player.setHealth(0.0F); // Mata al jugador
                    dangerZoneEnteredAt.remove(playerId); // Limpia el estado
                    playerWarned.put(playerId, false); // Limpia el warning para la próxima vez
                    sendDangerZonePacket(player, 0);
                }
            }
        }
    }

    private void sendDangerZonePacket(ServerPlayerEntity player, int secondsLeft) {
        ServerPlayNetworking.send(player, new DangerZonePayload(secondsLeft));
    }
}
