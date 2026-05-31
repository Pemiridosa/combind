package net.pemiridosa.combind.mixin;

import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.network.chat.Component;
import net.pemiridosa.combind.api.CombindKeyBinding;
import net.pemiridosa.combind.impl.CombindRegistry;
import net.pemiridosa.combind.mixin.accessor.AbstractSelectionListAccessor;
import net.pemiridosa.combind.mixin.accessor.KeyEntryAccessor;
import net.pemiridosa.combind.ui.CombindAlternativeEntry;
import org.spongepowered.asm.mixin.Mixin;
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
    @Inject(method = "resetMappingAndUpdateButtons", at = @At("RETURN"))
    private void combind$insertAlternatives(CallbackInfo ci) {
        AbstractSelectionListAccessor acc = (AbstractSelectionListAccessor)(Object)this;

        // Snapshot current entries (vanilla primaries + any stale alternatives from a previous call)
        @SuppressWarnings("unchecked")
        List<KeyBindsList.Entry> snapshot = new ArrayList<>((List<KeyBindsList.Entry>)(List<?>)acc.getChildren());

        // Rebuild the list, inserting fresh alternative entries after each primary
        acc.invokeClearEntries();

        KeyBindsList self = (KeyBindsList)(Object)this;

        for (KeyBindsList.Entry entry : snapshot) {
            if (entry instanceof CombindAlternativeEntry) continue;

            @SuppressWarnings("unchecked")
            AbstractSelectionList.Entry<?> e = (AbstractSelectionList.Entry<?>) entry;
            acc.invokeAddEntry(e, 20);

            if (entry instanceof KeyEntryAccessor keyEntry) {
                CombindKeyBinding binding = CombindRegistry.INSTANCE.get(keyEntry.getKey());
                if (binding != null && binding.comboCount() > 1) {
                    Component actionName = Component.translatable(keyEntry.getKey().getName());
                    for (int i = 1; i < binding.comboCount(); i++) {
                        acc.invokeAddEntry(new CombindAlternativeEntry(self, binding, i, actionName), 20);
                    }
                }
            }
        }
    }
}
