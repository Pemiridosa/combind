package net.pemiridosa.combind.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.network.chat.Component;
import net.pemiridosa.combind.api.CombindKeyBinding;
import net.pemiridosa.combind.impl.CombindConfig;
import net.pemiridosa.combind.mixin.accessor.KeyEntryAccessor;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CombindAlternativeEntry extends KeyBindsList.KeyEntry {
    private static final int REMOVE_BUTTON_WIDTH = 20;
    private static final int BUTTON_GAP = 5;

    private final KeyBindsList list;
    private final CombindKeyBinding binding;
    private final int comboIndex;
    private final Button removeButton;

    public CombindAlternativeEntry(KeyBindsList list, CombindKeyBinding binding, int comboIndex, Component actionName) {
        list.super(binding.getVanillaMapping(), Component.literal(""));

        this.list = list;
        this.binding = binding;
        this.comboIndex = comboIndex;

        this.removeButton = Button.builder(
            Component.literal("§c-"),
            _ -> removeAlternative()
        )
        .size(REMOVE_BUTTON_WIDTH, 20)
        .tooltip(Tooltip.create(Component.translatable("combind.ui.removeAlternativeBind")))
        .build();
    }

    @Override
    public void extractContent(@NonNull GuiGraphicsExtractor extractor, int mouseX, int mouseY, boolean active, float partialTick) {
        super.extractContent(extractor, mouseX, mouseY, active, partialTick);

        Button changeBtn = ((KeyEntryAccessor) this).getChangeButton();

        removeButton.setPosition(
            changeBtn.getX() - BUTTON_GAP - REMOVE_BUTTON_WIDTH,
            changeBtn.getY()
        );

        removeButton.extractRenderState(extractor, mouseX, mouseY, partialTick);
    }

    @Override
    public @NonNull List<? extends GuiEventListener> children() {
        List<GuiEventListener> children = new ArrayList<>(super.children());

        children.add(removeButton);

        return Collections.unmodifiableList(children);
    }

    @Override
    public @NonNull List<? extends NarratableEntry> narratables() {
        List<NarratableEntry> entries = new ArrayList<>(super.narratables());

        entries.add(removeButton);

        return Collections.unmodifiableList(entries);
    }

    private void removeAlternative() {
        binding.removeCombo(comboIndex);

        CombindConfig.save();

        list.resetMappingAndUpdateButtons();
    }
}
