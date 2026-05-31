package net.pemiridosa.combind.mixin;

import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.pemiridosa.combind.api.CombindKeyBinding;
import net.pemiridosa.combind.impl.CombindRegistry;
import net.pemiridosa.combind.mixin.accessor.KeyEntryAccessor;
import net.pemiridosa.combind.ui.CombindAlternativeEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * After each {@code resetMappingAndUpdateButtons()} call, inserts
 * {@link CombindAlternativeEntry} rows immediately after the primary entry
 * for any binding that has alternative combos.
 */
@Mixin(KeyBindsList.class)
public abstract class KeyBindsListMixin {
    @Shadow protected abstract void clearEntries();
    @Shadow protected abstract int addEntry(KeyBindsList.Entry entry, int height);

    @Inject(method = "resetMappingAndUpdateButtons", at = @At("RETURN"))
    private void combind$insertAlternatives(CallbackInfo ci) {
        // Snapshot current entries (vanilla primaries + any stale alternatives)
        List<KeyBindsList.Entry> snapshot = new ArrayList<>(((KeyBindsList)(Object)this).children());

        // Remove stale alternative entries, keep primary entries
        clearEntries();

        KeyBindsList self = (KeyBindsList)(Object)this;

        for (KeyBindsList.Entry entry : snapshot) {
            if (entry instanceof CombindAlternativeEntry) continue; // re-insert fresh below

            addEntry(entry, 20);

            // After each primary KeyEntry, insert fresh alternatives
            if (entry instanceof KeyEntryAccessor keyEntry) {
                CombindKeyBinding binding = CombindRegistry.INSTANCE.get(keyEntry.getKey());
                if (binding != null && binding.comboCount() > 1) {
                    for (int i = 1; i < binding.comboCount(); i++) {
                        addEntry(new CombindAlternativeEntry(self, binding, i, keyEntry.getName()), 20);
                    }
                }
            }
        }
    }
}
