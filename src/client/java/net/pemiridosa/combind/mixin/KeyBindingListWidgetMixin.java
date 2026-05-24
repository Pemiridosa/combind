package net.pemiridosa.combind.mixin;

import net.pemiridosa.combind.api.CombindKeyBinding;
import net.pemiridosa.combind.impl.CombindRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Patches the {@code Controls... > Key Binds...} screen's key-binding entry
 * to display the Combind combo display name (e.g. "Shift + A", "W W") instead of the
 * raw vanilla key name.
 *
 * <p>Conflict highlighting (yellow sidebar) is handled entirely by vanilla —
 * it reads the {@code hasCollision} flag which is set by {@code refreshEntry()}
 * via calls to {@link KeyMapping#same(KeyMapping)}, which we override in
 * {@link KeyMappingMixin} to compare Combind combos.
 */
@Mixin(KeyBindsList.KeyEntry.class)
public abstract class KeyBindingListWidgetMixin {
    @Redirect(
        method = "refreshEntry()V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/KeyMapping;getTranslatedKeyMessage()Lnet/minecraft/network/chat/Component;"
        )
    )
    private Component combind$getTranslatedKeyMessage(KeyMapping key) {
        CombindKeyBinding binding = CombindRegistry.INSTANCE.get(key);

        if (binding == null)
            return key.getTranslatedKeyMessage();

        return Component.literal(binding.getComboDisplayName());
    }
}
