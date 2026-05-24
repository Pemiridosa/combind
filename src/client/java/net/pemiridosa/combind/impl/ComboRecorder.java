package net.pemiridosa.combind.impl;

import net.pemiridosa.combind.api.CombindKeyBinding;
import net.pemiridosa.combind.api.InputKey;
import net.pemiridosa.combind.api.KeyCombo;
import org.lwjgl.glfw.GLFW;

import java.util.*;

/**
 * Captures user input during the rebinding phase in the {@code Controls... > Key Binds...} screen.
 *
 * <h2>Recording protocol</h2>
 * <ol>
 *   <li>Call {@link #startRecording(CombindKeyBinding)} when the user clicks a binding row.</li>
 *   <li>Feed key events via {@link #onKey(int, int)} and mouse events via
 *       {@link #onMouseButton(int, int)}.</li>
 *   <li>Poll {@link #isFinished()}; when {@code true} call {@link #finish()}
 *       to obtain the captured {@link KeyCombo}.</li>
 * </ol>
 *
 * <h2>Combo detection rules</h2>
 * <ul>
 *   <li><b>Chord</b>: user holds multiple keys; last key released is the trigger,
 *       earlier keys are modifiers.</li>
 *   <li><b>Sequence</b>: same key pressed N times within
 *       {@link #SEQUENCE_RECORDING_WINDOW_MS} ms without holding anything else.</li>
 *   <li><b>Mouse button</b>: single click immediately finalizes the combo.</li>
 * </ul>
 */
public final class ComboRecorder {
    public static final ComboRecorder INSTANCE = new ComboRecorder();

    private static final long SEQUENCE_RECORDING_WINDOW_MS = 600L;

    private boolean recording = false;
    private boolean finished = false;

    private CombindKeyBinding targetBinding = null;

    private final LinkedHashSet<InputKey> held = new LinkedHashSet<>();
    private final List<InputKey> pressOrder = new ArrayList<>();

    private InputKey lastSequenceKey = null;
    private int sequenceCount = 0;
    private long lastSequenceTime = 0;

    private KeyCombo result = null;

    private ComboRecorder() {}

    public void startRecording(CombindKeyBinding binding) {
        targetBinding = binding;

        startRecording();
    }

    public void startRecording() {
        recording = true;
        finished = false;
        result = null;

        held.clear();
        pressOrder.clear();

        lastSequenceKey = null;
        sequenceCount = 0;
        lastSequenceTime = 0;
    }

    public boolean isRecording() {
        return recording;
    }

    public CombindKeyBinding getTargetBinding() {
        return targetBinding;
    }

    public boolean isFinished() {
        return finished;
    }

    public KeyCombo finish() {
        recording = false;
        finished = false;
        targetBinding = null;

        KeyCombo r = result != null
            ? result
            : KeyCombo.unbound();

        result = null;

        return r;
    }

    /** Feed a keyboard event. Returns {@code true} if consumed. */
    public boolean onKey(int key, int action) {
        if (!recording)
            return false;

        if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
            result = KeyCombo.unbound();
            finished = true;
            recording = false;

            return true;
        }

        InputKey iKey = InputKey.keyboard(key);

        if (action == GLFW.GLFW_PRESS) {
            long now = System.currentTimeMillis();

            if (iKey.equals(lastSequenceKey) && held.isEmpty() && (now - lastSequenceTime) <= SEQUENCE_RECORDING_WINDOW_MS) {
                sequenceCount++;
            } else {
                lastSequenceKey = iKey;
                sequenceCount = 1;
            }

            lastSequenceTime = now;

            held.add(iKey);

            if (!pressOrder.contains(iKey))
                pressOrder.add(iKey);
        } else if (action == GLFW.GLFW_RELEASE) {
            held.remove(iKey);

            if (held.isEmpty()) {
                if (sequenceCount > 1 && pressOrder.size() == 1) {
                    result = KeyCombo.sequence(lastSequenceKey, sequenceCount);
                } else {
                    InputKey trigger = pressOrder.getLast();

                    InputKey[] mods = pressOrder
                        .subList(0, pressOrder.size() - 1)
                        .toArray(InputKey[]::new);

                    result = new KeyCombo(trigger, mods, 1);
                }

                finished  = true;
                recording = false;
            }
        }

        return true;
    }

    /**
     * Feed a mouse button event. A press immediately finalises as a single
     * mouse-button combo (no chord or sequence support for mouse during recording).
     *
     * @return {@code true} if consumed.
     */
    public boolean onMouseButton(int button, int action) {
        if (!recording) return false;
        if (action != GLFW.GLFW_PRESS) return true; // swallow release too
        // Any keyboard keys currently held become modifiers (e.g. Shift + Mouse Left)
        InputKey[] mods = held.toArray(InputKey[]::new);
        result    = new KeyCombo(InputKey.mouse(button), mods, 1);
        finished  = true;
        recording = false;
        return true;
    }

    // ── Preview (kept for possible future display use) ────────────────────────

    public String getPreview() {
        if (!recording) return "";
        if (sequenceCount > 0 && held.isEmpty()) return buildSequencePreview();
        if (!held.isEmpty()) return buildChordPreview();
        return "…";
    }

    private String buildChordPreview() {
        List<String> parts = new ArrayList<>();
        for (InputKey k : pressOrder) {
            if (held.contains(k)) parts.add(k.displayName());
        }
        return String.join(" + ", parts);
    }

    private String buildSequencePreview() {
        String name = lastSequenceKey != null ? lastSequenceKey.displayName() : "?";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sequenceCount; i++) {
            if (i > 0) sb.append(' ');
            sb.append(name);
        }
        return sb.toString();
    }
}
