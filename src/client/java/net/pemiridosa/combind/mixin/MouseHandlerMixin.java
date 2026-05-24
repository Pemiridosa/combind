package net.pemiridosa.combind.mixin;

import net.pemiridosa.combind.impl.ComboInputTracker;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Intercepts raw mouse button events so that Combind combos with mouse button
 * triggers (e.g. Mouse Left, Shift + Mouse Right) are tracked in-game, the
 * same way keyboard events are fed to {@link ComboInputTracker} via
 * {@link KeyboardHandlerMixin}.
 *
 * We do NOT cancel, so vanilla key-mapping and screen dispatch continue normally.
 */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Inject(
        method = "onButton(JLnet/minecraft/client/input/MouseButtonInfo;I)V",
        at = @At("HEAD")
    )
    private void combind$onButton(long window, MouseButtonInfo buttonInfo, int action, CallbackInfo ci) {
        ComboInputTracker.INSTANCE.onMouseButton(buttonInfo.button(), action);
    }
}
