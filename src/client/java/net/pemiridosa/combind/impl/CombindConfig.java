package net.pemiridosa.combind.impl;

import com.google.gson.*;
import net.pemiridosa.combind.CombindClient;
import net.pemiridosa.combind.config.CombindConfigData;
import net.pemiridosa.combind.api.CombindKeyBinding;
import net.pemiridosa.combind.api.KeyCombo;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;

/**
 * Reads and writes Combind settings from/to
 * {@code .minecraft/config/combind.json}.
 *
 * <h2>JSON structure</h2>
 * <pre>
 * {
 *   "config": {
 *     "allowConflicts": true,
 *     "sequenceWindowMs": 400,
 *     "sequenceRecordingWindowMs": 600,
 *     ...
 *   },
 *   "keyBinds": {
 *     "key.attack": { ... },
 *     ...
 *   }
 * }
 * </pre>
 */
public final class CombindConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(CombindClient.MOD_ID + "/config");

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    private static final Path PATH = FabricLoader
        .getInstance()
        .getConfigDir().resolve(CombindClient.MOD_ID + ".json");

    private CombindConfig() {}

    public static CombindConfigData config = new CombindConfigData();

    private static boolean ready = false;

    public static void save() {
        // Guard against Options.save() firing during startup before load() has run,
        // which would overwrite the config file with an empty JSON object.
        if (!ready) return;

        JsonObject keyBinds = new JsonObject();

        for (CombindKeyBinding b : CombindRegistry.INSTANCE.getAll()) {
            keyBinds.add(
                b.getVanillaMapping().getName(),
                b.getCombo().toJson()
            );
        }

        JsonObject root = new JsonObject();

        root.add("config", GSON.toJsonTree(config));
        root.add("keyBinds", keyBinds);

        try {
            Files.writeString(PATH, GSON.toJson(root));
        } catch (IOException e) {
            LOGGER.error("Failed to save " + CombindClient.MOD_ID + " config", e);
        }
    }

    public static void load() {
        if (!Files.exists(PATH))
            return;

        try {
            JsonObject root = JsonParser
                .parseString(Files.readString(PATH))
                .getAsJsonObject();

            if (root.has("config"))
                config = GSON.fromJson(root.get("config"), CombindConfigData.class);

            JsonObject keyBinds = root.has("keyBinds") && root.get("keyBinds").isJsonObject()
                ? root.getAsJsonObject("keyBinds")
                : new JsonObject();

            for (CombindKeyBinding b : CombindRegistry.INSTANCE.getAll()) {
                String name = b.getVanillaMapping().getName();

                if (!keyBinds.has(name))
                    continue;

                JsonElement el = keyBinds.get(name);

                if (!el.isJsonObject())
                    continue;

                try {
                    b.setCombo(KeyCombo.fromJson(el.getAsJsonObject()));
                } catch (Exception e) {
                    LOGGER.warn("Could not parse combo for '{}', skipping: {}", name, e.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load " + CombindClient.MOD_ID + " config", e);
        }

        ready = true;
    }
}
