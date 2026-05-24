package net.pemiridosa.combind.mixin;

import net.pemiridosa.combind.impl.ComboInputTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mirrors vanilla's {@code KeyMapping.releaseAll()} call in {@code Minecraft.setScreen()}:
 * when a screen opens, we clear all active combo bindings so that held keys do not
 * continue triggering actions (e.g. moving while inventory is open).
 */
@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(
        method = "setScreen(Lnet/minecraft/client/gui/screens/Screen;)V",
        at = @At("HEAD")
    )
    private void combind$onSetScreen(Screen screen, CallbackInfo ci) {
        if (screen != null) {
            ComboInputTracker.INSTANCE.releaseAll();
        }
    }
}
