package com.newdoge.positioning;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Positioning implements ModInitializer {

    public static final String MOD_ID = "positioning";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {

        PayloadTypeRegistry.playS2C().register(DangerZonePayload.ID, DangerZonePayload.CODEC);
        ModMethods.register();

        LOGGER.info("Mod Positioning inicializado correctamente!");

    }

}
