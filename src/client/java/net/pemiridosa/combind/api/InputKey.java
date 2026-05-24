package net.pemiridosa.combind.api;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

/**
 * A physical input: either a keyboard key or a mouse button.
 *
 * <p>Using an explicit tagged type instead of an offset-encoded int makes
 * the input source self-describing and eliminates the risk of passing a
 * mouse-button code to GLFW functions that only accept keyboard codes.
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * InputKey k = InputKey.keyboard(GLFW.GLFW_KEY_A);
 * InputKey m = InputKey.mouse(GLFW.GLFW_MOUSE_BUTTON_LEFT);
 * }</pre>
 */
public sealed interface InputKey permits InputKey.Keyboard, InputKey.Mouse {
    /** A keyboard key, identified by its GLFW key code. */
    record Keyboard(int glfwCode) implements InputKey {
        @Override
        public boolean isUnknown() {
            return glfwCode == GLFW.GLFW_KEY_UNKNOWN;
        }

        @Override
        public String displayName() {
            return keyboardName(glfwCode);
        }
    }

    /** A mouse button, identified by its GLFW button code (0 = left, 1 = right, 2 = middle, …). */
    record Mouse(int glfwButton) implements InputKey {
        @Override
        public boolean isUnknown() {
            return false;
        }

        @Override
        public String displayName() {
            return mouseName(glfwButton);
        }
    }

    // ── Contracts ─────────────────────────────────────────────────────────────

    boolean isUnknown();
    String displayName();

    // ── Factories ─────────────────────────────────────────────────────────────

    static InputKey keyboard(int glfwCode) {
        return new Keyboard(glfwCode);
    }

    static InputKey mouse(int glfwButton) {
        return new Mouse(glfwButton);
    }

    static InputKey unknown() {
        return new Keyboard(GLFW.GLFW_KEY_UNKNOWN);
    }

    // ── Serialization ─────────────────────────────────────────────────────────

    default JsonObject toJson() {
        JsonObject o = new JsonObject();

        switch (this) {
            case Keyboard k -> {
                o.addProperty("type", "keyboard");
                o.addProperty("code", k.glfwCode());
            }
            case Mouse m -> {
                o.addProperty("type", "mouse");
                o.addProperty("code", m.glfwButton());
            }
        }

        return o;
    }

    static InputKey fromJson(JsonObject o) {
        int code = o.get("code").getAsInt();

        return "mouse".equals(o.get("type").getAsString())
            ? mouse(code)
            : keyboard(code);
    }

    // ── Display helpers ───────────────────────────────────────────────────────

    static String keyboardName(int key) {
        return InputConstants.Type.KEYSYM.getOrCreate(key).getDisplayName().getString();
    }

    static String mouseName(int button) {
        return InputConstants.Type.MOUSE.getOrCreate(button).getDisplayName().getString();
    }
}
