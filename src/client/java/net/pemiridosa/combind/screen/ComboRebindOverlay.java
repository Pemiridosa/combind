package net.pemiridosa.combind.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.pemiridosa.combind.api.CombindKeyBinding;
import net.pemiridosa.combind.api.KeyCombo;
import net.pemiridosa.combind.impl.ComboRecorder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A lightweight overlay shown when the player clicks a Combind binding in the
 * Controls screen. It instructs the user to press their desired combination,
 * captures it via {@link ComboRecorder}, then applies and dismisses.
 *
 * <p>Opened via {@link #open(CombindKeyBinding, Runnable)}.
 */
public class ComboRebindOverlay extends Screen {

    // ── Fields ────────────────────────────────────────────────────────────────

    private final CombindKeyBinding binding;
    private final Screen parent;
    private final Runnable onDone;

    private static final Component TITLE =
        Component.translatable("combind.screen.rebind.title");
    private static final Component HINT =
        Component.translatable("combind.screen.rebind.hint");
    private static final Component HINT_ESCAPE =
        Component.translatable("combind.screen.rebind.hint_escape");
    private static final Component HINT_SEQUENCE =
        Component.translatable("combind.screen.rebind.hint_sequence");

    // ── Construction / opening ────────────────────────────────────────────────

    private ComboRebindOverlay(CombindKeyBinding binding, Screen parent, Runnable onDone) {
        super(TITLE);
        this.binding = binding;
        this.parent  = parent;
        this.onDone  = onDone;
    }

    /**
     * Open the overlay on top of the current screen.
     *
     * @param binding the binding being reassigned
     * @param onDone  callback fired after the combo is applied
     */
    public static void open(CombindKeyBinding binding, Runnable onDone) {
        Minecraft mc = Minecraft.getInstance();
        Screen current = mc.screen;
        mc.setScreen(new ComboRebindOverlay(binding, current, onDone));
        ComboRecorder.INSTANCE.startRecording();
    }

    // ── Screen lifecycle ──────────────────────────────────────────────────────

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (ComboRecorder.INSTANCE.isFinished()) {
            KeyCombo newCombo = ComboRecorder.INSTANCE.finish();
            binding.setCombo(newCombo);
            onDone.run();
            Minecraft.getInstance().setScreen(parent);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Dim background
        extractTransparentBackground(graphics);

        int cx = width / 2;
        int cy = height / 2;

        // Panel background
        int panelW = 300, panelH = 110;
        int px = cx - panelW / 2, py = cy - panelH / 2;
        graphics.fill(px, py, px + panelW, py + panelH, 0xCC000000);
        graphics.outline(px, py, panelW, panelH, 0xFF888888);

        // Title
        graphics.centeredText(
            font,
            Component.translatable(
                "combind.screen.rebind.binding",
                Component.translatable(binding.getVanillaMapping().getName())
            ),
            cx,
            py + 8,
            0xFFFFFF
        );

        // Live preview of keys being recorded
        String preview = ComboRecorder.INSTANCE.getPreview();
        Component previewText = preview.isBlank()
                ? Component.translatable("combind.screen.rebind.waiting")
                : Component.literal(preview);
        graphics.centeredText(font, previewText, cx, py + 30, 0xFFDD00);

        // Hints
        graphics.centeredText(font, HINT,          cx, py + 55, 0xAAAAAA);
        graphics.centeredText(font, HINT_SEQUENCE, cx, py + 68, 0xAAAAAA);
        graphics.centeredText(font, HINT_ESCAPE,   cx, py + 81, 0xAAAAAA);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // All key input is routed through KeyboardHandlerMixin → ComboRecorder
        // We just need to prevent the screen from handling Escape itself.
        return true; // consume
    }
}
