package net.pemiridosa.combind.api;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import net.pemiridosa.combind.impl.GlfwKeyCodes;

/**
 * A physical input: either a keyboard key or a mouse button.
 *
 * <p>Using an explicit tagged type instead of an offset-encoded int makes
 * the input source self-describing and eliminates the risk of passing a
 * mouse-button code to functions that only accept keyboard codes.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * InputKey k = InputKey.keyboard(InputConstants.KEY_A);
 * InputKey m = InputKey.mouse(InputConstants.MOUSE_BUTTON_LEFT);
 * }</pre>
 */
public sealed interface InputKey permits InputKey.Keyboard, InputKey.Mouse {
    /** A keyboard key, identified by its SDL scancode. */
    record Keyboard(int scancode) implements InputKey {
        @Override
        public boolean isUnknown() {
            return scancode == InputConstants.UNKNOWN.getValue();
        }

        @Override
        public String displayName() {
            return keyboardName(scancode);
        }
    }

    /** A mouse button, identified by its SDL button code (1 = left, 2 = middle, 3 = right, …). */
    record Mouse(int button) implements InputKey {
        @Override
        public boolean isUnknown() {
            return false;
        }

        @Override
        public String displayName() {
            return mouseName(button);
        }
    }

    // ── Contracts ─────────────────────────────────────────────────────────────

    boolean isUnknown();
    String displayName();

    // ── Factories ─────────────────────────────────────────────────────────────

    static InputKey keyboard(int scancode) {
        return new Keyboard(scancode);
    }

    static InputKey mouse(int button) {
        return new Mouse(button);
    }

    static InputKey unknown() {
        return new Keyboard(InputConstants.UNKNOWN.getValue());
    }

    /** Resolves a vanilla key name such as {@code key.keyboard.a}. */
    static InputKey fromName(String name) {
        InputConstants.Key key = InputConstants.getKey(name);

        return key.getType() == InputConstants.Type.MOUSE
            ? mouse(key.getValue())
            : keyboard(key.getValue());
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    /**
     * Keys are stored by name, since SDL and GLFW number them differently.
     * The GLFW code is written alongside it so versions before 26.3 can still read the config.
     */
    default JsonObject toJson() {
        JsonObject o = new JsonObject();

        switch (this) {
            case Keyboard k -> {
                String name = InputConstants.Type.KEYBOARD.getOrCreate(k.scancode()).getName();

                o.addProperty("type", "keyboard");
                o.addProperty("code", GlfwKeyCodes.code(name));
                o.addProperty("key", name);
            }
            case Mouse m -> {
                String name = InputConstants.Type.MOUSE.getOrCreate(m.button()).getName();

                o.addProperty("type", "mouse");
                o.addProperty("code", GlfwKeyCodes.code(name));
                o.addProperty("key", name);
            }
        }

        return o;
    }

    static InputKey fromJson(JsonObject o) {
        if (o.has("key"))
            return fromName(o.get("key").getAsString());

        // Written before 26.3: "code" is a GLFW code
        int code = o.get("code").getAsInt();

        return "mouse".equals(o.get("type").getAsString())
            ? fromName(GlfwKeyCodes.mouseName(code))
            : fromName(GlfwKeyCodes.keyboardName(code));
    }

    // ── Display helpers ───────────────────────────────────────────────────────

    static String keyboardName(int key) {
        return InputConstants.Type.KEYBOARD.getOrCreate(key).getDisplayName().getString();
    }

    static String mouseName(int button) {
        return InputConstants.Type.MOUSE.getOrCreate(button).getDisplayName().getString();
    }
}
