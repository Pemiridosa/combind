package net.pemiridosa.combind.impl;

import net.pemiridosa.combind.api.CombindKeyBinding;
import net.pemiridosa.combind.api.CombindKeyBinding.PressContext;
import net.pemiridosa.combind.api.InputKey;
import net.pemiridosa.combind.api.KeyCombo;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * Tracks held keys/buttons and evaluates combos on every press/release.
 *
 * <p>Mirrors vanilla's input gating: combos only activate when no screen is
 * open ({@code Minecraft.screen == null}), matching the same gate that vanilla
 * uses for {@code KeyMapping.set()} inside {@code KeyboardHandler.keyPress()}.
 * When a screen opens, {@link #releaseAll()} is called (mirroring vanilla's
 * {@code KeyMapping.releaseAll()} in {@code Minecraft.setScreen()}).
 */
public final class ComboInputTracker {
    public static final ComboInputTracker INSTANCE = new ComboInputTracker();

    private final Set<InputKey> heldKeys = new HashSet<>();
    private final Map<InputKey, SeqState> seqStates = new HashMap<>();

    // Maps each active binding to the specific combo that triggered it (for correct release detection).
    private final Map<CombindKeyBinding, KeyCombo> activeBindings = new LinkedHashMap<>();

    private record SeqState(long lastPressMs, int count) {}
    private record MatchResult(CombindKeyBinding binding, KeyCombo combo) {}

    private ComboInputTracker() {}

    /**
     * Mirrors {@code KeyMapping.releaseAll()}: called when a screen opens so
     * that any in-flight combo activations are canceled.
     */
    public void releaseAll() {
        for (CombindKeyBinding b : activeBindings.keySet()) {
            b.setActive(false);
        }

        activeBindings.clear();
        heldKeys.clear();
    }

    public void onKey(int key, int action) {
        InputKey iKey = InputKey.keyboard(key);

        if (action == GLFW.GLFW_PRESS) {
            heldKeys.add(iKey);

            handlePress(iKey);
        } else if (action == GLFW.GLFW_RELEASE) {
            heldKeys.remove(iKey);

            handleRelease(iKey);
        }

        // REPEAT intentionally ignored
    }

    public void onMouseButton(int button, int action) {
        InputKey iKey = InputKey.mouse(button);

        if (action == GLFW.GLFW_PRESS) {
            heldKeys.add(iKey);

            handlePress(iKey);
        } else if (action == GLFW.GLFW_RELEASE) {
            heldKeys.remove(iKey);

            handleRelease(iKey);
        }
    }


    /**
     * Mirrors vanilla: {@code KeyboardHandler.keyPress()} only calls
     * {@code KeyMapping.set(key, true)} when no screen is open.
     * We apply the same gate here.
     */
    private void handlePress(InputKey key) {
        if (Minecraft.getInstance().screen != null)
            return;

        long now = System.currentTimeMillis();
        int sequenceCount = advanceSequence(key, now);
        List<MatchResult> matched = collectMatches(key, sequenceCount);
        List<MatchResult> toFire = resolveConflicts(matched);

        for (MatchResult match : toFire)
            activate(match, key, now);
    }

    private int advanceSequence(InputKey key, long now) {
        SeqState prev = seqStates.get(key);

        int count = (prev != null && (now - prev.lastPressMs()) <= CombindConfig.config.sequenceWindowMs.get())
            ? prev.count() + 1
            : 1;

        seqStates.put(key, new SeqState(now, count));

        return count;
    }

    // For each binding, find the best-matching combo (most keys, then lowest sequence count).
    // Returns at most one MatchResult per binding.
    private List<MatchResult> collectMatches(InputKey key, int sequenceCount) {
        List<MatchResult> matched = new ArrayList<>();

        for (CombindKeyBinding binding : CombindRegistry.INSTANCE.getByTrigger(key)) {
            KeyCombo best = null;

            for (KeyCombo combo : binding.getCombos()) {
                if (!matches(combo, key, sequenceCount)) continue;

                if (best == null
                        || comboKeySet(combo).size() > comboKeySet(best).size()
                        || (comboKeySet(combo).size() == comboKeySet(best).size()
                            && combo.sequenceCount() < best.sequenceCount())) {
                    best = combo;
                }
            }

            if (best != null)
                matched.add(new MatchResult(binding, best));
        }

        return matched;
    }

    // When conflicts are OFF, only fire the most specific matched combos.
    // Specificity: most keys first, then lowest sequence count within the same key set
    // (a single press should win over a double-tap — no waiting for a tap that may never come).
    // Deduplicates by key set so two identical combos don't both fire.
    private static List<MatchResult> resolveConflicts(List<MatchResult> matched) {
        if (CombindConfig.config.allowConflicts.get())
            return matched;

        int maxKeys = matched.stream()
            .mapToInt(m -> comboKeySet(m.combo()).size())
            .max()
            .orElse(0);

        int minSeq = matched.stream()
            .filter(m -> comboKeySet(m.combo()).size() == maxKeys)
            .mapToInt(m -> m.combo().sequenceCount())
            .min()
            .orElse(1);

        Set<Set<InputKey>> seen = new HashSet<>();
        List<MatchResult> result = new ArrayList<>();

        for (MatchResult m : matched) {
            Set<InputKey> ks = comboKeySet(m.combo());

            if (ks.size() == maxKeys && m.combo().sequenceCount() == minSeq && seen.add(ks))
                result.add(m);
        }

        return result;
    }

    private void activate(MatchResult match, InputKey triggerKey, long now) {
        CombindKeyBinding binding = match.binding();
        KeyCombo combo = match.combo();

        binding.setActive(true);
        binding.addClick();

        PressContext ctx = new PressContext(binding, false);

        for (var cb : binding.getPressCallbacks()) {
            try {
                cb.accept(ctx);
            } catch (Exception ignored) {}
        }

        activeBindings.put(binding, combo);

        if (combo.isSequence())
            seqStates.put(triggerKey, new SeqState(now, 0));
    }

    private void handleRelease(InputKey key) {
        Iterator<Map.Entry<CombindKeyBinding, KeyCombo>> it = activeBindings.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<CombindKeyBinding, KeyCombo> entry = it.next();
            CombindKeyBinding binding = entry.getKey();
            KeyCombo combo = entry.getValue();

            if (shouldDeactivate(combo, key)) {
                binding.setActive(false);

                PressContext ctx = new PressContext(binding, true);

                for (var cb : binding.getReleaseCallbacks()) {
                    try {
                        cb.accept(ctx);
                    } catch (Exception ignored) {}
                }

                it.remove();
            }
        }
    }

    private boolean shouldDeactivate(KeyCombo combo, InputKey key) {
        if (combo.triggerKey().equals(key))
            return true;

        for (InputKey mod : combo.modifiers())
            if (mod.equals(key))
                return true;

        return false;
    }

    private static Set<InputKey> comboKeySet(KeyCombo combo) {
        Set<InputKey> keys = new HashSet<>(Arrays.asList(combo.modifiers()));

        keys.add(combo.triggerKey());

        return keys;
    }

    private boolean matches(KeyCombo combo, InputKey trigger, int sequenceSoFar) {
        if (!combo.triggerKey().equals(trigger))
            return false;

        if (combo.isSequence() && sequenceSoFar != combo.sequenceCount())
            return false;

        for (InputKey mod : combo.modifiers()) {
            if (!heldKeys.contains(mod))
                return false;
        }

        return true;
    }
}
