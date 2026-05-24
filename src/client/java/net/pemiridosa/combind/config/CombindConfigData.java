package net.pemiridosa.combind.config;

import net.minecraft.client.OptionInstance;
import net.pemiridosa.combind.config.entry.BooleanEntry;
import net.pemiridosa.combind.config.entry.ConfigEntry;
import net.pemiridosa.combind.config.entry.LongEntry;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

public class CombindConfigData {
    public final BooleanEntry allowConflicts = new BooleanEntry(
        "combind.config.allowConflicts",
        true
    );

    public final LongEntry sequenceWindowMs = new LongEntry(
        "combind.config.sequenceWindowMs",
        400L, 50L, 1000L
    );

    public final LongEntry sequenceRecordingWindowMs = new LongEntry(
        "combind.config.sequenceRecordingWindowMs",
        600L, 50L, 1000L
    );

    /** Returns all entries keyed by their JSON field name, in declaration order. */
    public Map<String, ConfigEntry<?>> entries() {
        Map<String, ConfigEntry<?>> map = new LinkedHashMap<>();
        for (Field f : getClass().getDeclaredFields()) {
            if (ConfigEntry.class.isAssignableFrom(f.getType())) {
                try {
                    map.put(f.getName(), (ConfigEntry<?>) f.get(this));
                } catch (IllegalAccessException ignored) {}
            }
        }
        return map;
    }

    public OptionInstance<?>[] asOptions() {
        return entries().values().stream()
            .map(ConfigEntry::asOption)
            .toArray(OptionInstance[]::new);
    }
}
