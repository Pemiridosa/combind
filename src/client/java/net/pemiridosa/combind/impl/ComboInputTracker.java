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
        if (Minecraft.getInstance().screen != null)
            return;

        long now = System.currentTimeMillis();

        SeqState prev = seqStates.get(key);

        int newCount = (prev != null && (now - prev.lastPressMs()) <= CombindConfig.config.sequenceWindowMs.get())
            ? prev.count() + 1
            : 1;

        seqStates.put(
            key,
            new SeqState(now, newCount)
        );

        Set<KeyCombo> firedCombos = CombindConfig.config.allowConflicts.get()
            ? null
            : new HashSet<>();

        for (CombindKeyBinding binding : CombindRegistry.INSTANCE.getByTrigger(key)) {
            KeyCombo combo = binding.getCombo();

            if (firedCombos != null && firedCombos.contains(combo))
                continue;

            if (!matches(combo, key, newCount))
                continue;

            binding.setActive(true);
            binding.addClick();

            PressContext ctx = new PressContext(binding, false);

            for (var cb : binding.getPressCallbacks()) {
                try {
                    cb.accept(ctx);
                } catch (Exception ignored) {}
            }

            activeBindings.add(binding);

            if (firedCombos != null)
                firedCombos.add(combo);

            if (combo.isSequence())
                seqStates.put(
                    key,
                    new SeqState(now, 0)
                );
        }
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
