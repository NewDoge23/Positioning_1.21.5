package com.newdoge.positioning;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerGroupManager {
    private static final Map<UUID, Integer> playerGroups = new HashMap<>();
    private static final Gson gson = new Gson();
    private static File dataFile;

    // Llama esto al iniciar el server
    public static void init(File worldDir) {
        dataFile = new File(worldDir, "player_groups.json");
        load();
    }

    public static void load() {
        if (dataFile != null && dataFile.exists()) {
            try (FileReader reader = new FileReader(dataFile)) {
                Type type = new TypeToken<Map<UUID, Integer>>(){}.getType();
                Map<UUID, Integer> loaded = gson.fromJson(reader, type);
                if (loaded != null) playerGroups.putAll(loaded);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void save() {
        if (dataFile != null) {
            try (FileWriter writer = new FileWriter(dataFile)) {
                gson.toJson(playerGroups, writer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static Integer getGroup(UUID uuid) {
        return playerGroups.get(uuid);
    }

    public static void setGroup(UUID uuid, int group) {
        playerGroups.put(uuid, group);
        save();
    }

    public static boolean hasGroup(UUID uuid) {
        return playerGroups.containsKey(uuid);
    }
}
