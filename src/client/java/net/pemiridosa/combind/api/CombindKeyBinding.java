package net.pemiridosa.combind.api;

import com.mojang.blaze3d.platform.InputConstants;
import net.pemiridosa.combind.impl.CombindRegistry;
import net.minecraft.client.KeyMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Wraps a vanilla {@link KeyMapping} with Combind combo support.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * // In your client initialiser:
 * CombindKeyBinding binding = CombindKeyBinding.of(
 *     new KeyMapping("key.mymod.myaction", GLFW.GLFW_KEY_A, "key.categories.mymod")
 * );
 *
 * binding.onPress(ctx -> System.out.println("pressed!"));
 * binding.onRelease(ctx -> System.out.println("released!"));
 *
 * // Add alternative combos:
 * binding.addCombo(new KeyCombo(InputKey.keyboard(GLFW.GLFW_KEY_F)));
 * }</pre>
 *
 * <p>Register your {@link KeyMapping} with Fabric's {@code KeyBindingHelper} as usual.
 * Combind will automatically track the assigned {@link KeyCombo} for it.
 */
public final class CombindKeyBinding {
    private final KeyMapping vanillaMapping;

    /** All combos for this binding. Index 0 is the primary combo. */
    private final List<KeyCombo> combos = new ArrayList<>();

    // In-game activation state — read by KeyMappingMixin to drive isDown()/consumeClick()
    private volatile boolean active = false;
    private volatile int pendingClicks = 0;

    // Callbacks
    private final List<Consumer<PressContext>> pressCallbacks = new ArrayList<>();
    private final List<Consumer<PressContext>> releaseCallbacks = new ArrayList<>();

    private CombindKeyBinding(KeyMapping vanillaMapping, KeyCombo initialCombo) {
        this.vanillaMapping = vanillaMapping;
        this.combos.add(initialCombo);
    }

    /**
     * Create a {@link CombindKeyBinding} from a vanilla {@link KeyMapping},
     * using its current key code as the initial (single-key) combo.
     *
     * <p>The binding is automatically registered with {@link CombindRegistry}.
     * You still need to register {@code vanillaMapping} with Fabric's KeyBindingHelper.
     */
    public static CombindKeyBinding of(KeyMapping vanillaMapping) {
        InputConstants.Key def = vanillaMapping.getDefaultKey();

        InputKey initialKey;

        if (def.getType() == InputConstants.Type.KEYSYM && def.getValue() != InputConstants.UNKNOWN.getValue()) {
            initialKey = InputKey.keyboard(def.getValue());
        } else if (def.getType() == InputConstants.Type.MOUSE) {
            initialKey = InputKey.mouse(def.getValue());
        } else {
            initialKey = InputKey.unknown();
        }

        return of(vanillaMapping, new KeyCombo(initialKey));
    }

    /**
     * Create a {@link CombindKeyBinding} with an explicit initial combo.
     */
    public static CombindKeyBinding of(KeyMapping vanillaMapping, KeyCombo initialCombo) {
        CombindKeyBinding binding = new CombindKeyBinding(vanillaMapping, initialCombo);

        CombindRegistry.INSTANCE.register(binding);

        return binding;
    }

    // ── API ───────────────────────────────────────────────────────────────────

    /** Returns the underlying vanilla {@link KeyMapping}. */
    public KeyMapping getVanillaMapping() {
        return vanillaMapping;
    }

    /** Returns the primary combo (index 0). */
    public KeyCombo getCombo() {
        return combos.get(0);
    }

    /** Returns an unmodifiable view of all combos for this binding. */
    public List<KeyCombo> getCombos() {
        return Collections.unmodifiableList(combos);
    }

    /** Returns the number of combos (primary + alternatives). */
    public int comboCount() {
        return combos.size();
    }

    /**
     * Sets the combo at the given index. Index 0 is the primary combo.
     * Resets activation state.
     */
    public void setCombo(int index, KeyCombo combo) {
        combos.set(index, combo);
        active = false;
        pendingClicks = 0;
    }

    /** Sets the primary combo. Shorthand for {@code setCombo(0, combo)}. */
    public void setCombo(KeyCombo combo) {
        setCombo(0, combo);
    }

    /**
     * Adds an alternative combo. Returns {@code this} for chaining.
     */
    public CombindKeyBinding addCombo(KeyCombo combo) {
        combos.add(combo);
        return this;
    }

    /**
     * Removes the combo at the given index. Index 0 (primary) cannot be removed.
     * Does nothing if {@code index} is out of range or 0.
     */
    public void removeCombo(int index) {
        if (index <= 0 || index >= combos.size()) return;
        combos.remove(index);
    }

    // ── In-game state (used by KeyMappingMixin) ───────────────────────────────

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public void addClick() {
        pendingClicks++;
    }

    /** Consume one pending click, returning true if there was one. */
    public boolean consumeClick() {
        if (pendingClicks <= 0)
            return false;

        pendingClicks--;

        return true;
    }

    /**
     * Register a callback that fires whenever this binding is triggered (pressed).
     *
     * @param callback receives a {@link PressContext} with info about the event.
     * @return {@code this} for chaining.
     */
    public CombindKeyBinding onPress(Consumer<PressContext> callback) {
        pressCallbacks.add(callback);

        return this;
    }

    /**
     * Register a callback that fires when the trigger key is released
     * (only if the binding was actively held).
     *
     * @param callback receives a {@link PressContext} with info about the event.
     * @return {@code this} for chaining.
     */
    public CombindKeyBinding onRelease(Consumer<PressContext> callback) {
        releaseCallbacks.add(callback);

        return this;
    }

    /** Unmodifiable view of press callbacks (used internally). */
    public List<Consumer<PressContext>> getPressCallbacks() {
        return Collections.unmodifiableList(pressCallbacks);
    }

    /** Unmodifiable view of release callbacks (used internally). */
    public List<Consumer<PressContext>> getReleaseCallbacks() {
        return Collections.unmodifiableList(releaseCallbacks);
    }

    // ── Display ───────────────────────────────────────────────────────────────

    /** Returns the display name of the primary combo, e.g. {@code "Left Shift + A"}. */
    public String getComboDisplayName() {
        return getCombo().getDisplayName();
    }

    @Override
    public String toString() {
        return "CombindKeyBinding{" + vanillaMapping.getName() + " -> " + combos + "}";
    }

    // ── Inner Types ───────────────────────────────────────────────────────────

    /**
     * Context object passed to press/release callbacks.
     */
    public record PressContext(
        CombindKeyBinding binding,
        boolean isRelease // True if this is a release event, false for press.
    ) {
        public KeyMapping vanillaMapping() {
            return binding.getVanillaMapping();
        }

        public KeyCombo combo() {
            return binding.getCombo();
        }
    }
}
