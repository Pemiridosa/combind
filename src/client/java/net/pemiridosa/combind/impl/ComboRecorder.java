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
 *       earlier keys are modifiers. Finalizes immediately when all keys are released.</li>
 *   <li><b>Sequence</b>: same single key pressed N times within
 *       {@code CombindConfig.sequenceRecordingWindowMs} ms. After each release the recorder
 *       enters a <em>pending</em> state: if the same key arrives again in time the
 *       count increments; otherwise the result is committed on the next frame after
 *       the window expires.</li>
 *   <li><b>Mouse button</b>: immediately finalizes with any currently held keyboard
 *       keys as modifiers (e.g. Left Shift + Left Button).</li>
 * </ul>
 */
public final class ComboRecorder {
    public static final ComboRecorder INSTANCE = new ComboRecorder();

    // ── State ─────────────────────────────────────────────────────────────────

    private boolean recording = false;
    private boolean finished = false;

    private CombindKeyBinding targetBinding = null;

    private final LinkedHashSet<InputKey> held = new LinkedHashSet<>();
    private final List<InputKey> pressOrder = new ArrayList<>();

    private InputKey lastSequenceKey = null;
    private int sequenceCount = 0;
    private long lastSequenceTime = 0;

    /**
     * True after a single key is fully released: we're waiting to see if the
     * same key is pressed again (sequence) before the window expires.
     */
    private boolean pendingFinalize = false;
    private long pendingTime = 0;

    private KeyCombo result = null;

    private ComboRecorder() {}

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    public void startRecording(CombindKeyBinding binding) {
        targetBinding = binding;

        startRecording();
    }

    public void startRecording() {
        recording = true;
        finished = false;
        result = null;
        pendingFinalize = false;
        pendingTime = 0;

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

    /**
     * Returns true when a result is ready. Also commits a pending single-key
     * result once the sequence window has expired — called every frame via the
     * render-state hook so timeouts are detected promptly.
     */
    public boolean isFinished() {
        if (finished)
            return true;

        if (pendingFinalize && System.currentTimeMillis() - pendingTime > CombindConfig.config.sequenceRecordingWindowMs.get())
            buildAndFinalize();

        return finished;
    }

    public KeyCombo finish() {
        recording = false;
        finished = false;
        pendingFinalize = false;
        targetBinding = null;

        KeyCombo r = result != null
            ? result
            : KeyCombo.unbound();

        result = null;

        return r;
    }

    // ── Input ─────────────────────────────────────────────────────────────────

    /** Feed a keyboard event. Returns {@code true} if consumed. */
    public boolean onKey(int key, int action) {
        if (!recording)
            return false;

        if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS) {
            result = KeyCombo.unbound();
            pendingFinalize = false;
            finished = true;
            recording = false;

            return true;
        }

        InputKey iKey = InputKey.keyboard(key);

        if (action == GLFW.GLFW_PRESS) {
            long now = System.currentTimeMillis();

            if (pendingFinalize) {
                // We released a key and are waiting: is this the same key in time?
                if (iKey.equals(lastSequenceKey) && (now - pendingTime) <= CombindConfig.config.sequenceRecordingWindowMs.get()) {
                    // Continue the sequence
                    pendingFinalize = false;
                    sequenceCount++;
                    lastSequenceTime = now;

                    held.add(iKey);
                } else {
                    // Different key or window expired — commit what we have, discard this press
                    buildAndFinalize();
                }

                return true;
            }

            // Normal first-press tracking
            if (iKey.equals(lastSequenceKey) && held.isEmpty() && (now - lastSequenceTime) <= CombindConfig.config.sequenceRecordingWindowMs.get()) {
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
                if (pressOrder.size() > 1) {
                    // Chord: all keys released — finalize immediately
                    buildAndFinalize();
                } else {
                    // Single key: wait to see if it's pressed again (sequence)
                    pendingFinalize = true;
                    pendingTime = System.currentTimeMillis();
                }
            }
        }

        return true;
    }

    /**
     * Feed a mouse button event. Immediately finalizes with any held keyboard
     * keys as modifiers.
     */
    public boolean onMouseButton(int button, int action) {
        if (!recording)
            return false;

        if (action != GLFW.GLFW_PRESS)
            return true;

        InputKey[] mods = held.toArray(InputKey[]::new);

        result = new KeyCombo(InputKey.mouse(button), mods, 1);
        pendingFinalize = false;
        finished = true;
        recording = false;

        return true;
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void buildAndFinalize() {
        if (sequenceCount > 1 && pressOrder.size() == 1) {
            result = KeyCombo.sequence(lastSequenceKey, sequenceCount);
        } else {
            InputKey trigger = pressOrder.getLast();

            InputKey[] mods = pressOrder
                .subList(0, pressOrder.size() - 1)
                .toArray(InputKey[]::new);

            result = new KeyCombo(trigger, mods, 1);
        }

        pendingFinalize = false;
        finished = true;
        recording = false;
    }

    // ── Preview ───────────────────────────────────────────────────────────────

    public String getPreview() {
        if (!recording)
            return "";

        if (sequenceCount > 0 && held.isEmpty())
            return buildSequencePreview();

        if (!held.isEmpty())
            return buildChordPreview();

        return "…";
    }

    private String buildChordPreview() {
        List<String> parts = new ArrayList<>();

        for (InputKey k : pressOrder)
            if (held.contains(k))
                parts.add(k.displayName());

        return String.join(" + ", parts);
    }

    private String buildSequencePreview() {
        String name = lastSequenceKey != null
            ? lastSequenceKey.displayName()
            : "?";

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < sequenceCount; i++) {
            if (i > 0)
                sb.append(' ');

            sb.append(name);
        }

        return sb.toString();
    }
}
