package com.newdoge.positioning;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Positioning implements ModInitializer {

    public static final String MOD_ID = "positioning";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {

        ModMethods.register();

        LOGGER.info("Mod Positioning inicializado correctamente!");

    }

}
