package net.pemiridosa.combind.impl;

import java.util.HashMap;
import java.util.Map;

/**
 * GLFW key and mouse button codes, used by Minecraft before 26.3, mapped to vanilla key names.
 *
 * <p>Minecraft 26.3 moved from GLFW to SDL, which numbers keys and mouse buttons differently.
 * Combos saved by older versions store GLFW codes, so they are translated by name on load.
 * Saved configs keep the GLFW code next to the key name so older versions can still read them.
 */
public final class GlfwKeyCodes {
    private static final String UNKNOWN = "key.keyboard.unknown";

    private static final Map<Integer, String> KEYBOARD_NAMES = new HashMap<>();
    private static final Map<Integer, String> MOUSE_NAMES = new HashMap<>();
    private static final Map<String, Integer> CODES = new HashMap<>();

    static {
        keyboard(-1, "key.keyboard.unknown");
        keyboard(32, "key.keyboard.space");
        keyboard(39, "key.keyboard.apostrophe");
        keyboard(44, "key.keyboard.comma");
        keyboard(45, "key.keyboard.minus");
        keyboard(46, "key.keyboard.period");
        keyboard(47, "key.keyboard.slash");
        keyboard(48, "key.keyboard.0");
        keyboard(49, "key.keyboard.1");
        keyboard(50, "key.keyboard.2");
        keyboard(51, "key.keyboard.3");
        keyboard(52, "key.keyboard.4");
        keyboard(53, "key.keyboard.5");
        keyboard(54, "key.keyboard.6");
        keyboard(55, "key.keyboard.7");
        keyboard(56, "key.keyboard.8");
        keyboard(57, "key.keyboard.9");
        keyboard(59, "key.keyboard.semicolon");
        keyboard(61, "key.keyboard.equal");
        keyboard(65, "key.keyboard.a");
        keyboard(66, "key.keyboard.b");
        keyboard(67, "key.keyboard.c");
        keyboard(68, "key.keyboard.d");
        keyboard(69, "key.keyboard.e");
        keyboard(70, "key.keyboard.f");
        keyboard(71, "key.keyboard.g");
        keyboard(72, "key.keyboard.h");
        keyboard(73, "key.keyboard.i");
        keyboard(74, "key.keyboard.j");
        keyboard(75, "key.keyboard.k");
        keyboard(76, "key.keyboard.l");
        keyboard(77, "key.keyboard.m");
        keyboard(78, "key.keyboard.n");
        keyboard(79, "key.keyboard.o");
        keyboard(80, "key.keyboard.p");
        keyboard(81, "key.keyboard.q");
        keyboard(82, "key.keyboard.r");
        keyboard(83, "key.keyboard.s");
        keyboard(84, "key.keyboard.t");
        keyboard(85, "key.keyboard.u");
        keyboard(86, "key.keyboard.v");
        keyboard(87, "key.keyboard.w");
        keyboard(88, "key.keyboard.x");
        keyboard(89, "key.keyboard.y");
        keyboard(90, "key.keyboard.z");
        keyboard(91, "key.keyboard.left.bracket");
        keyboard(92, "key.keyboard.backslash");
        keyboard(93, "key.keyboard.right.bracket");
        keyboard(96, "key.keyboard.grave.accent");
        keyboard(161, "key.keyboard.world.1");
        keyboard(162, "key.keyboard.world.2");
        keyboard(256, "key.keyboard.escape");
        keyboard(257, "key.keyboard.enter");
        keyboard(258, "key.keyboard.tab");
        keyboard(259, "key.keyboard.backspace");
        keyboard(260, "key.keyboard.insert");
        keyboard(261, "key.keyboard.delete");
        keyboard(262, "key.keyboard.right");
        keyboard(263, "key.keyboard.left");
        keyboard(264, "key.keyboard.down");
        keyboard(265, "key.keyboard.up");
        keyboard(266, "key.keyboard.page.up");
        keyboard(267, "key.keyboard.page.down");
        keyboard(268, "key.keyboard.home");
        keyboard(269, "key.keyboard.end");
        keyboard(280, "key.keyboard.caps.lock");
        keyboard(281, "key.keyboard.scroll.lock");
        keyboard(282, "key.keyboard.num.lock");
        keyboard(283, "key.keyboard.print.screen");
        keyboard(284, "key.keyboard.pause");
        keyboard(290, "key.keyboard.f1");
        keyboard(291, "key.keyboard.f2");
        keyboard(292, "key.keyboard.f3");
        keyboard(293, "key.keyboard.f4");
        keyboard(294, "key.keyboard.f5");
        keyboard(295, "key.keyboard.f6");
        keyboard(296, "key.keyboard.f7");
        keyboard(297, "key.keyboard.f8");
        keyboard(298, "key.keyboard.f9");
        keyboard(299, "key.keyboard.f10");
        keyboard(300, "key.keyboard.f11");
        keyboard(301, "key.keyboard.f12");
        keyboard(302, "key.keyboard.f13");
        keyboard(303, "key.keyboard.f14");
        keyboard(304, "key.keyboard.f15");
        keyboard(305, "key.keyboard.f16");
        keyboard(306, "key.keyboard.f17");
        keyboard(307, "key.keyboard.f18");
        keyboard(308, "key.keyboard.f19");
        keyboard(309, "key.keyboard.f20");
        keyboard(310, "key.keyboard.f21");
        keyboard(311, "key.keyboard.f22");
        keyboard(312, "key.keyboard.f23");
        keyboard(313, "key.keyboard.f24");
        keyboard(314, "key.keyboard.f25");
        keyboard(320, "key.keyboard.keypad.0");
        keyboard(321, "key.keyboard.keypad.1");
        keyboard(322, "key.keyboard.keypad.2");
        keyboard(323, "key.keyboard.keypad.3");
        keyboard(324, "key.keyboard.keypad.4");
        keyboard(325, "key.keyboard.keypad.5");
        keyboard(326, "key.keyboard.keypad.6");
        keyboard(327, "key.keyboard.keypad.7");
        keyboard(328, "key.keyboard.keypad.8");
        keyboard(329, "key.keyboard.keypad.9");
        keyboard(330, "key.keyboard.keypad.decimal");
        keyboard(331, "key.keyboard.keypad.divide");
        keyboard(332, "key.keyboard.keypad.multiply");
        keyboard(333, "key.keyboard.keypad.subtract");
        keyboard(334, "key.keyboard.keypad.add");
        keyboard(335, "key.keyboard.keypad.enter");
        keyboard(336, "key.keyboard.keypad.equal");
        keyboard(340, "key.keyboard.left.shift");
        keyboard(341, "key.keyboard.left.control");
        keyboard(342, "key.keyboard.left.alt");
        keyboard(343, "key.keyboard.left.win");
        keyboard(344, "key.keyboard.right.shift");
        keyboard(345, "key.keyboard.right.control");
        keyboard(346, "key.keyboard.right.alt");
        keyboard(347, "key.keyboard.right.win");
        keyboard(348, "key.keyboard.menu");

        mouse(0, "key.mouse.left");
        mouse(1, "key.mouse.right");
        mouse(2, "key.mouse.middle");
        mouse(3, "key.mouse.4");
        mouse(4, "key.mouse.5");
        mouse(5, "key.mouse.6");
        mouse(6, "key.mouse.7");
        mouse(7, "key.mouse.8");
    }

    private GlfwKeyCodes() {}

    /** Key name for a GLFW key code, or the unknown key if GLFW had no such code. */
    public static String keyboardName(int code) {
        return KEYBOARD_NAMES.getOrDefault(code, UNKNOWN);
    }

    /** Key name for a GLFW mouse button code, or the unknown key if GLFW had no such button. */
    public static String mouseName(int button) {
        return MOUSE_NAMES.getOrDefault(button, UNKNOWN);
    }

    /** GLFW code for a key name, or -1 for keys that GLFW did not have. */
    public static int code(String name) {
        return CODES.getOrDefault(name, -1);
    }

    private static void keyboard(int code, String name) {
        KEYBOARD_NAMES.put(code, name);
        CODES.put(name, code);
    }

    private static void mouse(int button, String name) {
        MOUSE_NAMES.put(button, name);
        CODES.put(name, button);
    }
}
