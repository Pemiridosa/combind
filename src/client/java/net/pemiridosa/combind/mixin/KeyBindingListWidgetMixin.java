package net.pemiridosa.combind.mixin;

import net.pemiridosa.combind.api.CombindKeyBinding;
import net.pemiridosa.combind.impl.CombindRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Patches the Controls screen's key-binding entry to display the Combind combo
 * display name (e.g. "Shift + A") instead of the raw vanilla key name.
 *
 * <p>Conflict highlighting (yellow sidebar) is handled entirely by vanilla —
 * it reads the {@code hasCollision} flag which is set by {@code refreshEntry()}
 * via calls to {@link KeyMapping#same(KeyMapping)}, which we override in
 * {@link KeyMappingMixin} to compare Combind combos.
 */
@Mixin(KeyBindsList.KeyEntry.class)
public abstract class KeyBindingListWidgetMixin {

    @Shadow @Final private KeyMapping key;
    @Shadow @Final private Button changeButton;

    @Inject(method = "refreshEntry()V", at = @At("TAIL"))
    private void combind$refreshEntry(CallbackInfo ci) {
        CombindKeyBinding binding = CombindRegistry.INSTANCE.get(key);
        if (binding == null) return;
        changeButton.setMessage(Component.literal(binding.getComboDisplayName()));
    }
}
