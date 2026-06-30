package com.playerfinder.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.playerfinder.PlayerFinder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Loads/saves the group tree + settings as pretty JSON under {@code config/playerfinder.json}. */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private FinderConfig config = new FinderConfig();

    public ConfigManager() {
        this.file = FabricLoader.getInstance().getConfigDir().resolve("playerfinder.json");
    }

    public FinderConfig get() {
        return config;
    }

    public void load() {
        try {
            if (Files.exists(file)) {
                FinderConfig loaded = GSON.fromJson(Files.readString(file), FinderConfig.class);
                config = (loaded != null) ? loaded : new FinderConfig();
            } else {
                config = new FinderConfig();
            }
        } catch (Exception e) {
            PlayerFinder.LOGGER.warn("[PlayerFinder] Failed to load config, using defaults", e);
            config = new FinderConfig();
        }
        // Defensive: never leave the tree null after a hand-edited/partial file.
        if (config.root == null) config.root = new FinderGroup("root");
        if (config.root.groups == null) config.root.groups = new java.util.ArrayList<>();
        save();   // normalise on disk (and create the file on first run)
    }

    public void save() {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(config));
        } catch (IOException e) {
            PlayerFinder.LOGGER.warn("[PlayerFinder] Failed to save config", e);
        }
    }
}
