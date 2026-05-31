package net.pemiridosa.combind.mixin;

import net.pemiridosa.combind.api.CombindKeyBinding;
import net.pemiridosa.combind.impl.CombindConfig;
import net.pemiridosa.combind.impl.CombindRegistry;
import net.pemiridosa.combind.impl.ComboRecorder;
import net.pemiridosa.combind.api.KeyCombo;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts the {@code Controls... > Key Binds...} screen's rebinding flow so
 * Combind bindings are recorded inline.
 *
 * <h2>Flow</h2>
 * <ol>
 *   <li>User clicks a change button → vanilla sets {@code selectedKey}.</li>
 *   <li>Our RETURN injection on {@code mouseClicked} (or {@code keyPressed}
 *       for keyboard-nav) detects that {@code selectedKey} is a Combind
 *       binding and starts {@link ComboRecorder} without clearing
 *       {@code selectedKey} — so the {@code Controls... > Key Binds...} screen
 *       keeps the button highlighted in vanilla's "waiting" style.</li>
 *   <li>{@link KeyboardHandlerMixin} cancels all keyboard events while
 *       recording, feeding them only to {@link ComboRecorder}.</li>
 *   <li>Every render frame our {@code extractRenderState} injection calls
 *       {@code keyBindsList.refreshEntries()} so the live preview in
 *       {@link KeyBindingListWidgetMixin} is updated.</li>
 *   <li>When recording finishes the combo is applied, config is saved, and
 *       {@code selectedKey} is cleared.</li>
 * </ol>
 */
@Mixin(KeyBindsScreen.class)
public abstract class KeyBindsScreenMixin {
    @Shadow public KeyMapping selectedKey;
    @Shadow private KeyBindsList keyBindsList;

    /**
     * When recording is active and the user clicks a mouse button, capture it
     * as the new binding immediately (before vanilla processes the click).
     */
    @Inject(
        method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z",
        at = @At("HEAD"),
        cancellable = true
    )
    private void combind$beforeMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        // Recording is handled in MouseHandlerMixin (fires for all buttons including side
        // buttons 3+, which never reach mouseClicked). We only need to cancel here so the
        // screen doesn't react to button presses mid-recording.
        // Check isFinished() too: for buttons 0-2, MouseHandlerMixin already completed
        // recording before this mouseClicked dispatch fires.
        if (ComboRecorder.INSTANCE.isRecording() || ComboRecorder.INSTANCE.isFinished()) {
            cir.setReturnValue(true);
            cir.cancel();
        }
    }

    @Inject(
        method = "mouseClicked(Lnet/minecraft/client/input/MouseButtonEvent;Z)Z",
        at = @At("RETURN")
    )
    private void combind$afterMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        combind$maybeStartRecording();
    }

    @Inject(
        method = "keyPressed(Lnet/minecraft/client/input/KeyEvent;)Z",
        at = @At("RETURN")
    )
    private void combind$afterKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        combind$maybeStartRecording();
    }

    @Inject(
        method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V",
        at = @At("HEAD")
    )
    private void combind$onRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (ComboRecorder.INSTANCE.isFinished()) {
            combind$applyFinishedRecording();
        }
    }


    /**
     * If {@code selectedKey} was just set to a Combind binding and we are not
     * already recording, start inline recording. We intentionally keep
     * {@code selectedKey} set so the Controls screen highlights the button.
     */
    @Unique
    private void combind$maybeStartRecording() {
        if (selectedKey == null)
            return;

        if (ComboRecorder.INSTANCE.isRecording())
            return;

        if (ComboRecorder.INSTANCE.isFinished())
            return; // wait for applyFinishedRecording

        CombindKeyBinding binding = CombindRegistry.INSTANCE.get(selectedKey);

        if (binding == null)
            return;

        ComboRecorder.INSTANCE.startRecording(binding);
    }

    @Unique
    private void combind$applyFinishedRecording() {
        CombindKeyBinding binding = ComboRecorder.INSTANCE.getTargetBinding();
        int comboIndex = ComboRecorder.INSTANCE.getTargetComboIndex();
        KeyCombo newCombo = ComboRecorder.INSTANCE.finish();

        if (binding == null)
            return;

        binding.setCombo(comboIndex, newCombo);
        CombindConfig.save();

        selectedKey = null;
        keyBindsList.resetMappingAndUpdateButtons();
    }
}
