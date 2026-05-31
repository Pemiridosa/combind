package net.pemiridosa.combind.mixin;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.network.chat.Component;
import net.pemiridosa.combind.api.CombindKeyBinding;
import net.pemiridosa.combind.api.KeyCombo;
import net.pemiridosa.combind.impl.CombindConfig;
import net.pemiridosa.combind.impl.CombindRegistry;
import net.pemiridosa.combind.ui.CombindAlternativeEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Mixin(KeyBindsList.KeyEntry.class)
public abstract class KeyBindingListWidgetMixin {
    @Shadow private Button changeButton;

    @Unique private KeyBindsList combind$list;
    @Unique private Button combind$plusButton;

    @Inject(
        method = "<init>(Lnet/minecraft/client/gui/screens/options/controls/KeyBindsList;Lnet/minecraft/client/KeyMapping;Lnet/minecraft/network/chat/Component;)V",
        at = @At("TAIL")
    )
    private void combind$onInit(KeyBindsList list, KeyMapping mapping, Component name, CallbackInfo ci) {
        if ((Object)this instanceof CombindAlternativeEntry) return;
        combind$list = list;

        CombindKeyBinding binding = CombindRegistry.INSTANCE.get(mapping);
        if (binding == null) return;

        combind$plusButton = Button.builder(
            Component.literal("+"),
            btn -> {
                binding.addCombo(KeyCombo.unbound());
                CombindConfig.save();
                combind$list.resetMappingAndUpdateButtons();
            }
        )
        .size(20, 20)
        .tooltip(Tooltip.create(Component.translatable("combind.ui.addAlternativeBind")))
        .build();
    }

    @Inject(
        method = "extractContent(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIZF)V",
        at = @At("TAIL")
    )
    private void combind$onExtractContent(GuiGraphicsExtractor extractor, int mouseX, int mouseY, boolean active, float partialTick, CallbackInfo ci) {
        if (combind$plusButton == null) return;
        combind$plusButton.setPosition(changeButton.getX() - 5 - combind$plusButton.getWidth(), changeButton.getY());
        combind$plusButton.extractRenderState(extractor, mouseX, mouseY, partialTick);
    }

    @Inject(method = "children()Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private void combind$addToChildren(CallbackInfoReturnable<List<GuiEventListener>> cir) {
        if (combind$plusButton == null) return;
        List<GuiEventListener> extended = new ArrayList<>(cir.getReturnValue());
        extended.add(combind$plusButton);
        cir.setReturnValue(Collections.unmodifiableList(extended));
    }

    @Inject(method = "narratables()Ljava/util/List;", at = @At("RETURN"), cancellable = true)
    private void combind$addToNarratables(CallbackInfoReturnable<List<NarratableEntry>> cir) {
        if (combind$plusButton == null) return;
        List<NarratableEntry> extended = new ArrayList<>(cir.getReturnValue());
        extended.add(combind$plusButton);
        cir.setReturnValue(Collections.unmodifiableList(extended));
    }
}
