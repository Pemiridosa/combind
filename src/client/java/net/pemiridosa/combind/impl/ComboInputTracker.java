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
    private final Set<CombindKeyBinding> activeBindings = new HashSet<>();

    private record SeqState(long lastPressMs, int count) {}

    private ComboInputTracker() {}

    /**
     * Mirrors {@code KeyMapping.releaseAll()}: called when a screen opens so
     * that any in-flight combo activations are canceled.
     */
    public void releaseAll() {
        for (CombindKeyBinding b : activeBindings) {
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
        if (Minecraft.getInstance().gui.screen() != null)
            return;

        long now = System.currentTimeMillis();
        int sequenceCount = advanceSequence(key, now);
        List<CombindKeyBinding> matched = collectMatches(key, sequenceCount);
        List<CombindKeyBinding> toFire = resolveConflicts(matched);

        for (CombindKeyBinding binding : toFire)
            activate(binding, key, now);
    }

    private int advanceSequence(InputKey key, long now) {
        SeqState prev = seqStates.get(key);

        int count = (prev != null && (now - prev.lastPressMs()) <= CombindConfig.config.sequenceWindowMs.get())
            ? prev.count() + 1
            : 1;

        seqStates.put(key, new SeqState(now, count));

        return count;
    }

    private List<CombindKeyBinding> collectMatches(InputKey key, int sequenceCount) {
        List<CombindKeyBinding> matched = new ArrayList<>();

        for (CombindKeyBinding binding : CombindRegistry.INSTANCE.getByTrigger(key))
            if (matches(binding.getCombo(), key, sequenceCount))
                matched.add(binding);

        return matched;
    }

    // When conflicts are OFF, only fire the most specific matched combos.
    // Specificity: most keys first, then lowest sequence count within the same key set
    // (a single press should win over a double-tap — no waiting for a tap that may never come).
    // Deduplicates by key set so two identical combos don't both fire.
    private static List<CombindKeyBinding> resolveConflicts(List<CombindKeyBinding> matched) {
        if (CombindConfig.config.allowConflicts.get())
            return matched;

        int maxKeys = matched.stream()
            .mapToInt(b -> comboKeySet(b.getCombo()).size())
            .max()
            .orElse(0);

        int minSeq = matched.stream()
            .filter(b -> comboKeySet(b.getCombo()).size() == maxKeys)
            .mapToInt(b -> b.getCombo().sequenceCount())
            .min()
            .orElse(1);

        Set<Set<InputKey>> seen = new HashSet<>();
        List<CombindKeyBinding> result = new ArrayList<>();

        for (CombindKeyBinding b : matched) {
            Set<InputKey> ks = comboKeySet(b.getCombo());

            if (ks.size() == maxKeys && b.getCombo().sequenceCount() == minSeq && seen.add(ks))
                result.add(b);
        }

        return result;
    }

    private void activate(CombindKeyBinding binding, InputKey triggerKey, long now) {
        binding.setActive(true);
        binding.addClick();

        PressContext ctx = new PressContext(binding, false);

        for (var cb : binding.getPressCallbacks()) {
            try {
                cb.accept(ctx);
            } catch (Exception ignored) {}
        }

        activeBindings.add(binding);

        if (binding.getCombo().isSequence())
            seqStates.put(triggerKey, new SeqState(now, 0));
    }

    private void handleRelease(InputKey key) {
        Iterator<CombindKeyBinding> it = activeBindings.iterator();

        while (it.hasNext()) {
            CombindKeyBinding binding = it.next();

            if (shouldDeactivate(binding.getCombo(), key)) {
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
