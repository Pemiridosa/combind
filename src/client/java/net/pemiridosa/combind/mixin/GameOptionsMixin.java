package net.pemiridosa.combind.mixin;

import net.pemiridosa.combind.impl.CombindConfig;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Hooks into {@link Options#save()} so that Combind combo settings are
 * persisted whenever the player saves game options (e.g. closing the Controls screen).
 */
@Mixin(Options.class)
public abstract class GameOptionsMixin {
    @Inject(method = "save()V", at = @At("TAIL"))
    private void combind$onSave(CallbackInfo ci) {
        CombindConfig.save();
    }
}
