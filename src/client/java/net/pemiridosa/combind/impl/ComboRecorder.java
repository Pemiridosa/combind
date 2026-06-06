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
 *   <li>Call {@link #startRecording()} when the user clicks a binding row.</li>
 *   <li>Feed key events via {@link #onKey(int, int)} and mouse events via
 *       {@link #onMouseButton(int, int)}.</li>
 *   <li>Poll {@link #isFinished()}; when {@code true} call {@link #finish()}
 *       to obtain the captured {@link KeyCombo}.</li>
 * </ol>
 *
 * <h2>Combo detection rules</h2>
 * <ul>
 *   <li><b>Chord</b>: user holds multiple keys; last key pressed is the trigger,
 *       earlier held keys are modifiers. Finalizes immediately when all keys are released.</li>
 *   <li><b>Sequence</b>: same single key pressed N times within
 *       {@code CombindConfig.sequenceRecordingWindowMs} ms. After each release the recorder
 *       enters a <em>pending</em> state: if the same key arrives again in time the
 *       count increments; otherwise the result is committed on the next frame after
 *       the window expires.</li>
 *   <li><b>Chord + sequence</b>: hold modifier(s), then tap the trigger key N times.
 *       Releasing the trigger while modifiers remain held enters the pending state.
 *       Finalizes when the window expires or the modifier(s) are released.</li>
 *   <li><b>Mouse button</b>: immediately finalizes with any currently held keyboard
 *       keys as modifiers (e.g. Left Shift + Left Button).</li>
 * </ul>
 */
public final class ComboRecorder {
    public static final ComboRecorder INSTANCE = new ComboRecorder();

    // ── State ─────────────────────────────────────────────────────────────────

    private boolean recording = false;
    private boolean finished = false;

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
                if (pendingFinalize) {
                    // Modifiers released while mid-sequence detection: let the timeout handle it.
                } else if (pressOrder.size() > 1 && sequenceCount == 1) {
                    // Pure chord (no sequence): finalize immediately
                    buildAndFinalize();
                } else {
                    // Single key or end of a sequence tap: wait for possible continuation
                    pendingFinalize = true;
                    pendingTime = System.currentTimeMillis();
                }
            } else if (!pendingFinalize && iKey.equals(lastSequenceKey)) {
                // Trigger released while modifiers still held → potential chord+sequence
                pendingFinalize = true;
                pendingTime = System.currentTimeMillis();
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
        if (sequenceCount > 1) {
            // Pure sequence (no modifiers) or chord+sequence — lastSequenceKey is the trigger
            InputKey[] mods = pressOrder.stream()
                .filter(k -> !k.equals(lastSequenceKey))
                .toArray(InputKey[]::new);

            result = new KeyCombo(lastSequenceKey, mods, sequenceCount);
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
}
