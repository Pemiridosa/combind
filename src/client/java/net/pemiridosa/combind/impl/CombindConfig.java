package net.pemiridosa.combind.impl;

import com.google.gson.*;
import net.pemiridosa.combind.CombindClient;
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
 * <h2>Config options</h2>
 * <ul>
 *   <li>{@code allowConflicts} — when {@code true} (default), multiple bindings
 *       assigned the same combo all fire simultaneously.  When {@code false},
 *       only the first registered binding fires.</li>
 * </ul>
 */
public final class CombindConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(CombindClient.MOD_ID + "/config");
    private static final Gson   GSON   = new GsonBuilder().setPrettyPrinting().create();
    private static final Path   PATH   = FabricLoader.getInstance()
            .getConfigDir().resolve("combind.json");

    private CombindConfig() {}

    // ── Settings ──────────────────────────────────────────────────────────────

    /**
     * When {@code true} (default), multiple bindings with the same combo all
     * fire at the same time.  Set to {@code false} to let the first registered
     * binding win and suppress the rest.
     */
    public static boolean allowConflicts = true;

    // ── Save ──────────────────────────────────────────────────────────────────

    public static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("allowConflicts", allowConflicts);
        for (CombindKeyBinding b : CombindRegistry.INSTANCE.getAll()) {
            root.add(b.getVanillaMapping().getName(), b.getCombo().toJson());
        }
        try {
            Files.writeString(PATH, GSON.toJson(root));
        } catch (IOException e) {
            LOGGER.error("Failed to save Combind config", e);
        }
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    public static void load() {
        if (!Files.exists(PATH)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(PATH)).getAsJsonObject();

            if (root.has("allowConflicts")) {
                allowConflicts = root.get("allowConflicts").getAsBoolean();
            }

            for (CombindKeyBinding b : CombindRegistry.INSTANCE.getAll()) {
                String name = b.getVanillaMapping().getName();
                if (!root.has(name)) continue;
                JsonElement el = root.get(name);
                if (!el.isJsonObject()) continue;
                try {
                    b.setCombo(KeyCombo.fromJson(el.getAsJsonObject()));
                } catch (Exception e) {
                    LOGGER.warn("Could not parse combo for '{}', skipping: {}", name, e.getMessage());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load Combind config", e);
        }
    }
}
