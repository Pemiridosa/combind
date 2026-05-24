package net.pemiridosa.combind.mixin;

import net.pemiridosa.combind.impl.ComboInputTracker;
import net.pemiridosa.combind.impl.ComboRecorder;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts keyboard events before Minecraft processes them,
 * feeding them to {@link ComboInputTracker} (for normal gameplay) and
 * {@link ComboRecorder} (when the overlay is recording a combo).
 *
 * In MC 26.1.2 the GLFW callback is wrapped into a {@link KeyEvent} record
 * and dispatched to the main thread as keyPress(long, int, KeyEvent).
 */
@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {

    /**
     * {@code keyPress} is the internal method called on the main thread for
     * every GLFW key event. Signature: (handle, action, KeyEvent).
     *
     * We inject at HEAD so we see the event before vanilla's own processing.
     */
    @Inject(
        method = "keyPress(JILnet/minecraft/client/input/KeyEvent;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void combind$onKeyPress(long handle, int action, KeyEvent event, CallbackInfo ci) {
        int key = event.key();

        // If the combo overlay is recording, let the recorder consume the event
        if (ComboRecorder.INSTANCE.isRecording()) {
            boolean consumed = ComboRecorder.INSTANCE.onKey(key, action);
            if (consumed) {
                ci.cancel(); // Don't let Minecraft interpret it as gameplay input
                return;
            }
        }

        // Normal gameplay: feed the tracker
        ComboInputTracker.INSTANCE.onKey(key, action);
    }
}
