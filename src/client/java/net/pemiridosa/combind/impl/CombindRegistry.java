package net.pemiridosa.combind.impl;

import net.pemiridosa.combind.api.CombindKeyBinding;
import net.pemiridosa.combind.api.InputKey;
import net.pemiridosa.combind.api.KeyCombo;
import net.minecraft.client.KeyMapping;

import java.util.*;

/**
 * Central registry that maps vanilla {@link KeyMapping} instances to their
 * {@link CombindKeyBinding} wrappers.
 */
public final class CombindRegistry {
    public static final CombindRegistry INSTANCE = new CombindRegistry();

    private final Map<KeyMapping, CombindKeyBinding> byMapping = new LinkedHashMap<>();
    private final List<CombindKeyBinding> all = new ArrayList<>();

    private CombindRegistry() {}

    /** Register a binding. Called automatically by {@link CombindKeyBinding#of}. */
    public void register(CombindKeyBinding binding) {
        if (!byMapping.containsKey(binding.getVanillaMapping())) {
            byMapping.put(binding.getVanillaMapping(), binding);

            all.add(binding);
        }
    }

    /** Look up the {@link CombindKeyBinding} for a given vanilla mapping, or {@code null}. */
    public CombindKeyBinding get(KeyMapping mapping) {
        return byMapping.get(mapping);
    }

    /** All registered bindings in registration order. */
    public List<CombindKeyBinding> getAll() {
        return Collections.unmodifiableList(all);
    }

    /**
     * Returns all bindings whose current combo trigger matches {@code key}.
     * Used by {@link ComboInputTracker} to quickly find candidates on each press.
     */
    public List<CombindKeyBinding> getByTrigger(InputKey key) {
        List<CombindKeyBinding> result = new ArrayList<>();

        for (CombindKeyBinding b : all) {
            for (KeyCombo c : b.getCombos()) {
                if (c.triggerKey().equals(key)) {
                    result.add(b);
                    break;
                }
            }
        }

        return result;
    }
}
