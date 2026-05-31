package net.pemiridosa.combind.mixin;

import net.pemiridosa.combind.impl.ComboInputTracker;
import net.pemiridosa.combind.impl.ComboRecorder;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts raw mouse button events so that Combind combos with mouse button
 * triggers (e.g. Left Button, Shift + Right Button) are tracked in-game, the
 * same way keyboard events are fed to {@link ComboInputTracker} via
 * {@link KeyboardHandlerMixin}.
 *
 * <p>Recording is handled here rather than in the screen's {@code mouseClicked}
 * hook because vanilla only dispatches {@code mouseClicked} to screens for
 * buttons 0–2. Side buttons (3+) never reach the screen hook, so they must be
 * captured here where all GLFW button events arrive.
 *
 * <p>Do NOT cancel, so vanilla key-mapping and screen dispatch continue normally.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(
        method = "onButton(JLnet/minecraft/client/input/MouseButtonInfo;I)V",
        at = @At("HEAD")
    )
    private void combind$onButton(long handle, MouseButtonInfo rawButtonInfo, int action, CallbackInfo ci) {
        int button = rawButtonInfo.button();

        if (ComboRecorder.INSTANCE.isRecording()) {
            ComboRecorder.INSTANCE.onMouseButton(button, action);
            return;
        }

        ComboInputTracker.INSTANCE.onMouseButton(button, action);
    }
}
