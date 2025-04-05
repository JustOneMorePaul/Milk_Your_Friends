package com.JOMPILS.milkyourfriends;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Config {
    private static final File CONFIG_FILE = new File("config/MilkYourFriends.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static ConfigData configData;

    public static void loadConfig() {
        if (!CONFIG_FILE.exists()) {
            configData = new ConfigData();
            saveConfig();
            return;
        }
        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            configData = GSON.fromJson(reader, ConfigData.class);
            // Fallback if the file is empty or the JSON didn't deserialize properly.
            if (configData == null) {
                configData = new ConfigData();
                saveConfig();
            }
        } catch (IOException e) {
            e.printStackTrace();
            configData = new ConfigData();
        }
}

    public static void saveConfig() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
                GSON.toJson(configData, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- Milking-specific getters and setters ---

    public static Set<UUID> getMilkPlayers() {
        return configData.milk.players;
    }

    public static String getMilkListType() {
        return configData.milk.listType;
    }

    public static void addMilkPlayer(UUID uuid) {
        configData.milk.players.add(uuid);
        saveConfig();
    }

    public static void removeMilkPlayer(UUID uuid) {
        configData.milk.players.remove(uuid);
        saveConfig();
    }

    public static void setMilkListType(String type) {
        configData.milk.listType = type;
        saveConfig();
    }

    // --- Config Structure ---

    public static class ConfigData {
        // Each ability gets its own configuration.
        public AbilityConfig milk = new AbilityConfig();
        public AbilityConfig shear = new AbilityConfig(); // For future use.
    }

    public static class AbilityConfig {
        // Default list type is Blacklist, all players will be milkable.
        public String listType = "blacklist";
        public Set<UUID> players = new HashSet<>();
    }
}
