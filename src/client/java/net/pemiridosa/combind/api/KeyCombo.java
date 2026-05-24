package net.pemiridosa.combind.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;

import java.util.*;

/**
 * Represents an immutable key combination binding for a single {@link KeyMapping}.
 *
 * <p>A {@code KeyCombo} can describe:
 * <ul>
 *   <li>A single key (e.g. {@code A}, {@code Mouse Left})</li>
 *   <li>A chord — multiple keys held simultaneously (e.g. {@code Shift+A})</li>
 *   <li>A sequence — the same key pressed multiple times quickly (e.g. {@code W W})</li>
 * </ul>
 *
 * <p>The input type (keyboard vs. mouse) is encoded in {@link InputKey}, so there
 * is no need for magic offset constants.
 *
 * @param triggerKey    The primary input that triggers this combo.
 * @param modifiers     Modifier / chord keys that must be held when {@link #triggerKey()} fires.
 *                      Stored in a canonical order for deterministic equality.
 * @param sequenceCount How many times {@link #triggerKey()} must be pressed in quick succession.
 *                      {@code 1} = single press.
 */
public record KeyCombo(InputKey triggerKey, InputKey[] modifiers, int sequenceCount) {
    /** Maximum time (ms) between presses in a sequence. */
    public static final long SEQUENCE_WINDOW_MS = 400L;

    // ── Constructors ─────────────────────────────────────────────────────────

    public KeyCombo(InputKey triggerKey) {
        this(triggerKey, new InputKey[0], 1);
    }

    public KeyCombo(InputKey triggerKey, InputKey[] modifiers) {
        this(triggerKey, modifiers, 1);
    }

    public KeyCombo(InputKey triggerKey, InputKey[] modifiers, int sequenceCount) {
        this.triggerKey = triggerKey;
        this.modifiers = modifiers.clone(); // preserve press order for display
        this.sequenceCount = Math.max(1, sequenceCount);
    }

    // ── Factories ─────────────────────────────────────────────────────────────

    public static KeyCombo unbound() {
        return new KeyCombo(InputKey.unknown());
    }

    public static KeyCombo sequence(InputKey key, int times) {
        return new KeyCombo(key, new InputKey[0], times);
    }

    // ── Queries ───────────────────────────────────────────────────────────────

    public boolean isUnbound() {
        return triggerKey.isUnknown();
    }

    public boolean hasModifiers() {
        return modifiers.length > 0;
    }

    public boolean isSequence() {
        return sequenceCount > 1;
    }

    // ── Display ───────────────────────────────────────────────────────────────

    public String getDisplayName() {
        if (isUnbound())
            return Component
                .translatable("key.keyboard.unknown")
                .getString();

        List<String> parts = new ArrayList<>();

        for (InputKey mod : modifiers)
            parts.add(mod.displayName());

        String trigger = triggerKey.displayName();

        if (isSequence()) {
            StringBuilder sb = new StringBuilder(trigger);

            for (int i = 1; i < sequenceCount; i++)
                sb.append(' ').append(trigger);

            String seq = sb.toString();

            return parts.isEmpty()
                ? seq
                : String.join(" + ", parts) + " + " + seq;
        } else {
            parts.add(trigger);

            return String.join(" + ", parts);
        }
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();

        obj.add("trigger", triggerKey.toJson());

        JsonArray mods = new JsonArray();

        for (InputKey m : modifiers)
            mods.add(m.toJson());

        obj.add("modifiers", mods);
        obj.addProperty("sequenceCount", sequenceCount);

        return obj;
    }

    public static KeyCombo fromJson(JsonObject obj) {
        // Trigger: new format {"type":"keyboard","code":76} OR legacy int field "triggerKey"
        InputKey trigger;

        if (obj.has("trigger")) {
            trigger = InputKey.fromJson(obj.getAsJsonObject("trigger"));
        } else {
            trigger = InputKey.keyboard(obj.get("triggerKey").getAsInt());
        }

        // Modifiers: new format [{...}] OR legacy [int, ...]
        InputKey[] mods = new InputKey[0];

        if (obj.has("modifiers")) {
            JsonArray arr = obj.getAsJsonArray("modifiers");

            mods = new InputKey[arr.size()];

            for (int i = 0; i < arr.size(); i++) {
                JsonElement el = arr.get(i);

                mods[i] = el.isJsonObject()
                    ? InputKey.fromJson(el.getAsJsonObject())
                    : InputKey.keyboard(el.getAsInt()); // legacy int
            }
        }

        int seq = obj.has("sequenceCount")
            ? obj.get("sequenceCount").getAsInt()
            : 1;

        return new KeyCombo(trigger, mods, seq);
    }

    // ── Conflict detection ────────────────────────────────────────────────────

    /**
     * Returns true if this combo and {@code other} share any key (trigger or modifier).
     * Used by {@code KeyMapping.same()} override to drive the Controls screen's
     * yellow conflict indicator.
     *
     * <p>Examples: {@code Shift+F} overlaps {@code F} (same trigger) and also
     * overlaps {@code Shift} (Shift is a modifier of one and trigger of the other).
     */
    public boolean overlaps(KeyCombo other) {
        if (this.isUnbound() || other.isUnbound())
            return false;

        // Build a set of all keys used by `other`
        Set<InputKey> otherKeys = new HashSet<>();

        otherKeys.add(other.triggerKey);
        otherKeys.addAll(Arrays.asList(other.modifiers));

        // Check if any key from `this` appears in `other`
        if (otherKeys.contains(this.triggerKey))
            return true;

        for (InputKey m : this.modifiers)
            if (otherKeys.contains(m))
                return true;

        return false;
    }

    // ── Object ────────────────────────────────────────────────────────────────

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (!(o instanceof KeyCombo other))
            return false;

        // Compare modifiers order-independently (sort copies, not the stored arrays)
        return triggerKey.equals(other.triggerKey)
            && Arrays.equals(sorted(modifiers), sorted(other.modifiers))
            && sequenceCount == other.sequenceCount;
    }

    @Override
    public int hashCode() {
        int h = triggerKey.hashCode();

        h = 31 * h + Arrays.hashCode(sorted(modifiers));
        h = 31 * h + sequenceCount;

        return h;
    }

    @Override
    public @NonNull String toString() {
        return "KeyCombo{" + getDisplayName() + "}";
    }

    // ── Internal ──────────────────────────────────────────────────────────────

    private static InputKey[] sorted(InputKey[] mods) {
        InputKey[] arr = mods.clone();

        Arrays.sort(arr, Comparator.comparingInt(KeyCombo::keyOrdinal));

        return arr;
    }

    static int keyOrdinal(InputKey k) {
        return switch (k) {
            case InputKey.Keyboard kb -> kb.glfwCode();
            case InputKey.Mouse    m  -> 0x40000 + m.glfwButton();
        };
    }
}
