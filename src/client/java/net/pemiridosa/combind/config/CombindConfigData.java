package net.pemiridosa.combind.config;

import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

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
        400L,
        50L,
        5000L
    );

    public final LongEntry sequenceRecordingWindowMs = new LongEntry(
        "combind.config.sequenceRecordingWindowMs",
        400L,
        50L,
        5000L
    );

    /** Returns all entries keyed by their JSON field name, in declaration order. */
    public Map<String, ConfigEntry<?>> entries() {
        Map<String, ConfigEntry<?>> map = new LinkedHashMap<>();

        for (Field f : getClass().getDeclaredFields()) {
            if (ConfigEntry.class.isAssignableFrom(f.getType())) {
                try {
                    map.put(
                        f.getName(),
                        (ConfigEntry<?>) f.get(this)
                    );
                } catch (IllegalAccessException ignored) {}
            }
        }

        return map;
    }

    public void addEntries(ConfigCategory category, ConfigEntryBuilder builder) {
        entries()
            .values()
            .forEach(entry -> entry.addTo(category, builder));
    }
}
