package com.newdoge.positioning;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import org.apache.logging.log4j.core.jmx.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

public class Positioning implements ModInitializer {

    public static final String MOD_ID = "positioning";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {

        PayloadTypeRegistry.playS2C().register(DangerZonePayload.ID, DangerZonePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(RequestGroupSelectionPayload.ID, RequestGroupSelectionPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(SubmitGroupSelectionPayload.ID, SubmitGroupSelectionPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SubmitGroupSelectionPayload.ID, (payload, context) -> {
            context.player().getServer().execute(() -> {
                int group = payload.group();
                PlayerGroupManager.setGroup(context.player().getUuid(), group);
                String msg = (group == 1) ?
                        "¡Te uniste al grupo Norte! Solo podés explorar el lado positivo del mapa (Z+)." :
                        "¡Te uniste al grupo Sur! Solo podés explorar el lado negativo del mapa (Z-).";
                context.player().sendMessage(Text.literal(msg), false);
                // Podés sumar más acciones aquí
            });
        });


        ModMethods.register();

        ServerLifecycleEvents.SERVER_STARTED.register((server) -> {
            PlayerGroupManager.init(server.getSavePath(WorldSavePath.ROOT).toFile());
            Positioning.LOGGER.info("PlayerGroupManager inicializado para el mundo en " + server.getSavePath(WorldSavePath.ROOT));
        });

        LOGGER.info("Mod Positioning inicializado correctamente!");

    }

}
