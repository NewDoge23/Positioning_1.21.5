package com.newdoge.positioning;

import com.newdoge.positioning.network.DangerZonePayload;
import com.newdoge.positioning.network.RequestGroupSelectionPayload;
import com.newdoge.positioning.network.SubmitGroupSelectionPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Positioning implements ModInitializer {

    public static final String MOD_ID = "positioning";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Registro de payloads
        PayloadTypeRegistry.playS2C().register(DangerZonePayload.ID, DangerZonePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RequestGroupSelectionPayload.ID, RequestGroupSelectionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SubmitGroupSelectionPayload.ID, SubmitGroupSelectionPayload.CODEC);

        // Recibir elección de grupo
        ServerPlayNetworking.registerGlobalReceiver(SubmitGroupSelectionPayload.ID, (payload, context) -> {
            context.player().getServer().execute(() -> {
                int group = payload.group();
                PlayerGroupManager.setGroup(context.player().getUuid(), group);
                String msg = (group == 1)
                        ? "positioning.joined_north"
                        : "positioning.joined_south";
                context.player().sendMessage(Text.translatable(msg), false);
            });
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
}
