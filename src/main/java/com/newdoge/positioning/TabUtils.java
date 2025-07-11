package com.newdoge.positioning;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;

import static com.newdoge.positioning.Positioning.LOGGER;

public class TabUtils {

    private static final String TEAM_NORTE = "norte";
    private static final String TEAM_SUR = "sur";

    public static void assignTeamForGroup(ServerPlayerEntity player, int group) {
        Scoreboard scoreboard = player.getServer().getScoreboard();

        Team teamNorte = scoreboard.getTeam("norte");
        if (teamNorte == null) {
            teamNorte = scoreboard.addTeam("norte");
        }
        Team teamSur = scoreboard.getTeam("sur");
        if (teamSur == null) {
            teamSur = scoreboard.addTeam("sur");
        }

        String uuidStr = player.getUuidAsString();

        // Si está en Norte pero debería estar en Sur, lo remueve de Norte.
        if (group == 2 && teamNorte.getPlayerList().contains(uuidStr)) {
            teamNorte.getPlayerList().remove(uuidStr);
            LOGGER.info("Removiendo de equipos a " + player.getName().getString() + " [" + uuidStr + "]");
        }
        // Si está en Sur pero debería estar en Norte, lo remueve de Sur.
        if (group == 1 && teamSur.getPlayerList().contains(uuidStr)) {
            teamSur.getPlayerList().remove(uuidStr);
            LOGGER.info("Removiendo de equipos a " + player.getName().getString() + " [" + uuidStr + "]");
        }
        // Si no debería estar en ningún grupo, lo remueve de ambos
        if (group != 1 && teamNorte.getPlayerList().contains(uuidStr)) {
            teamNorte.getPlayerList().remove(uuidStr);
            LOGGER.info("Removiendo de equipos a " + player.getName().getString() + " [" + uuidStr + "]");
        }
        if (group != 2 && teamSur.getPlayerList().contains(uuidStr)) {
            teamSur.getPlayerList().remove(uuidStr);
            LOGGER.info("Removiendo de equipos a " + player.getName().getString() + " [" + uuidStr + "]");
        }

        // Si no está en el equipo correcto, lo agrega
        if (group == 1) {
            teamNorte.getPlayerList().add(uuidStr);
            LOGGER.info("Agregando a " + player.getName().getString() + " [" + uuidStr + "] al grupo Norte");
        } else if (group == 2) {
            teamSur.getPlayerList().add(uuidStr);
            LOGGER.info("Agregando a " + player.getName().getString() + " [" + uuidStr + "] al grupo Sur");
        }

        // Si el grupo es inválido, no hace nada extra

        //LOGGER.info(player.getName().getString() + " [" + uuidStr + "] Agregado al grupo " + (group == 1 ? "Norte" : "Sur"));
    }
}