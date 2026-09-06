package net.metrodata.autoelytra.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.metrodata.autoelytra.AutoElytraBoost;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModConfig {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static Path configPath;
    public static ConfigData INSTANCE = new ConfigData();

    private ModConfig() {
    }

    public static void load() {
        configPath = FabricLoader.getInstance().getConfigDir().resolve("autoelytra.json");
        if (!Files.isRegularFile(configPath)) {
            save();
            return;
        }

        try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
            ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
            if (loaded != null) {
                loaded.priority = loaded.priority == null ? UsePriority.FLIGHT_LONG_FIRST : loaded.priority;
                INSTANCE = loaded;
            }
        } catch (IOException | RuntimeException e) {
            AutoElytraBoost.LOGGER.error("[Auto Elytra Boost] Failed to read config, using defaults.", e);
            INSTANCE = new ConfigData();
        }
    }

    public static void save() {
        if (configPath == null) {
            configPath = FabricLoader.getInstance().getConfigDir().resolve("autoelytra.json");
        }
        try {
            Files.createDirectories(configPath.getParent());
            try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
                GSON.toJson(INSTANCE, writer);
            }
        } catch (IOException e) {
            AutoElytraBoost.LOGGER.error("[Auto Elytra Boost] Failed to write config.", e);
        }
    }
}
