package com.newdoge.positioning;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Positioning implements ModInitializer {

    public static final String MOD_ID = "positioning";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Registro de payloads
        PayloadTypeRegistry.playS2C().register(DangerZonePayload.ID, DangerZonePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RequestGroupSelectionPayload.ID, RequestGroupSelectionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SubmitGroupSelectionPayload.ID, SubmitGroupSelectionPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(SyncGroupsPayload.ID, SyncGroupsPayload.CODEC);

        // Recibir elección de grupo y asignar equipo + sync visual tab
        ServerPlayNetworking.registerGlobalReceiver(SubmitGroupSelectionPayload.ID, (payload, context) -> {
            context.player().getServer().execute(() -> {
                int group = payload.group();
                PlayerGroupManager.setGroup(context.player().getUuid(), group);
                TabUtils.assignTeamForGroup(context.player(), group);

                String msg = (group == 1)
                        ? "¡Te uniste al grupo Norte! Solo podés explorar el lado positivo del mapa (Z+)."
                        : "¡Te uniste al grupo Sur! Solo podés explorar el lado negativo del mapa (Z-).";
                context.player().sendMessage(Text.literal(msg), false);

                // --- IMPORTANTE: Sync visual TAB a todos ---
                syncAllGroupsToAllPlayers(context.player().getServer());
            });
        });

        // Asignar equipo/tab al entrar (por si ya estaba guardado el grupo)
        ServerPlayerEvents.JOIN.register((player) -> {
            Integer group = PlayerGroupManager.getGroup(player.getUuid());
            if (group != null) {
                TabUtils.assignTeamForGroup(player, group);
            }
            // Sync visual TAB a todos
            syncAllGroupsToAllPlayers(player.getServer());
        });

        // Registro de lógica principal
        ModMethods.register();

        // Inicialización cuando arranca el server
        ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
            PlayerGroupManager.init(server.getSavePath(WorldSavePath.ROOT).toFile());
            LOGGER.info("PlayerGroupManager inicializado para el mundo en " + server.getSavePath(WorldSavePath.ROOT));
        });

        LOGGER.info("Mod Positioning inicializado correctamente!");
    }

    // ---- SYNC para el TAB visual ----
    public static void syncAllGroupsToAllPlayers(MinecraftServer server) {
        Map<UUID, Integer> groups = new HashMap<>();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            Integer group = PlayerGroupManager.getGroup(player.getUuid());
            if (group != null) {
                groups.put(player.getUuid(), group);
            }
        }
        SyncGroupsPayload payload = new SyncGroupsPayload(groups);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
