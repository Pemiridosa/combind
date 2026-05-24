package net.pemiridosa.combind.api;

import com.google.gson.JsonObject;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

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
        String name = GLFW.glfwGetKeyName(key, -1);

        if (name != null && !name.isEmpty())
            return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);

        return switch (key) {
            case GLFW.GLFW_KEY_LEFT_SHIFT, GLFW.GLFW_KEY_RIGHT_SHIFT     -> "Shift";
            case GLFW.GLFW_KEY_LEFT_CONTROL, GLFW.GLFW_KEY_RIGHT_CONTROL -> "Ctrl";
            case GLFW.GLFW_KEY_LEFT_ALT, GLFW.GLFW_KEY_RIGHT_ALT         -> "Alt";
            case GLFW.GLFW_KEY_LEFT_SUPER, GLFW.GLFW_KEY_RIGHT_SUPER     -> "Super";
            case GLFW.GLFW_KEY_SPACE        -> "Space";
            case GLFW.GLFW_KEY_ENTER        -> "Enter";
            case GLFW.GLFW_KEY_TAB          -> "Tab";
            case GLFW.GLFW_KEY_BACKSPACE    -> "Backspace";
            case GLFW.GLFW_KEY_INSERT       -> "Insert";
            case GLFW.GLFW_KEY_DELETE       -> "Delete";
            case GLFW.GLFW_KEY_HOME         -> "Home";
            case GLFW.GLFW_KEY_END          -> "End";
            case GLFW.GLFW_KEY_PAGE_UP      -> "Page Up";
            case GLFW.GLFW_KEY_PAGE_DOWN    -> "Page Down";
            case GLFW.GLFW_KEY_UP           -> "Up";
            case GLFW.GLFW_KEY_DOWN         -> "Down";
            case GLFW.GLFW_KEY_LEFT         -> "Left";
            case GLFW.GLFW_KEY_RIGHT        -> "Right";
            case GLFW.GLFW_KEY_ESCAPE       -> "Escape";
            case GLFW.GLFW_KEY_F1           -> "F1";
            case GLFW.GLFW_KEY_F2           -> "F2";
            case GLFW.GLFW_KEY_F3           -> "F3";
            case GLFW.GLFW_KEY_F4           -> "F4";
            case GLFW.GLFW_KEY_F5           -> "F5";
            case GLFW.GLFW_KEY_F6           -> "F6";
            case GLFW.GLFW_KEY_F7           -> "F7";
            case GLFW.GLFW_KEY_F8           -> "F8";
            case GLFW.GLFW_KEY_F9           -> "F9";
            case GLFW.GLFW_KEY_F10          -> "F10";
            case GLFW.GLFW_KEY_F11          -> "F11";
            case GLFW.GLFW_KEY_F12          -> "F12";
            case GLFW.GLFW_KEY_KP_0         -> "Num 0";
            case GLFW.GLFW_KEY_KP_1         -> "Num 1";
            case GLFW.GLFW_KEY_KP_2         -> "Num 2";
            case GLFW.GLFW_KEY_KP_3         -> "Num 3";
            case GLFW.GLFW_KEY_KP_4         -> "Num 4";
            case GLFW.GLFW_KEY_KP_5         -> "Num 5";
            case GLFW.GLFW_KEY_KP_6         -> "Num 6";
            case GLFW.GLFW_KEY_KP_7         -> "Num 7";
            case GLFW.GLFW_KEY_KP_8         -> "Num 8";
            case GLFW.GLFW_KEY_KP_9         -> "Num 9";
            case GLFW.GLFW_KEY_KP_ENTER     -> "Num Enter";
            case GLFW.GLFW_KEY_KP_ADD       -> "Num +";
            case GLFW.GLFW_KEY_KP_SUBTRACT  -> "Num -";
            case GLFW.GLFW_KEY_KP_MULTIPLY  -> "Num *";
            case GLFW.GLFW_KEY_KP_DIVIDE    -> "Num /";
            case GLFW.GLFW_KEY_UNKNOWN      -> "None";
            default                         -> "Key " + key;
        };
    }

    static String mouseName(int button) {
        return switch (button) {
            case GLFW.GLFW_MOUSE_BUTTON_LEFT   -> "Mouse Left";
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT  -> "Mouse Right";
            case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "Mouse Middle";
            default                            -> "Mouse " + (button + 1);
        };
    }
}
