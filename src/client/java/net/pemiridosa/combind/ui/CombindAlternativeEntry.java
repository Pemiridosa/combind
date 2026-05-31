package net.pemiridosa.combind.ui;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.Minecraft;
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
import net.pemiridosa.combind.impl.ComboRecorder;
import net.pemiridosa.combind.mixin.accessor.KeyBindsListAccessor;

import java.util.List;

/**
 * A list entry for an alternative combo of a {@link CombindKeyBinding}.
 * Shown below the primary entry with a "*" name prefix and a remove button.
 */
public class CombindAlternativeEntry extends KeyBindsList.Entry {
    private static final int BUTTON_HEIGHT = 20;
    private static final int RECORD_BUTTON_WIDTH = 75;
    private static final int REMOVE_BUTTON_WIDTH = 50;
    private static final int BUTTON_GAP = 5;
    private static final int RIGHT_MARGIN = 10;

    private final KeyBindsList list;
    private final CombindKeyBinding binding;
    private final int comboIndex;
    private final Component actionName;

    private final Button recordButton;
    private final Button removeButton;

    public CombindAlternativeEntry(KeyBindsList list, CombindKeyBinding binding, int comboIndex, Component actionName) {
        this.list = list;
        this.binding = binding;
        this.comboIndex = comboIndex;
        this.actionName = actionName;

        this.recordButton = Button.builder(
            getComboDisplayText(),
            btn -> startRecording()
        )
        .size(RECORD_BUTTON_WIDTH, BUTTON_HEIGHT)
        .build();

        this.removeButton = Button.builder(
            Component.literal("§c-"),
            btn -> removeAlternative()
        )
        .size(REMOVE_BUTTON_WIDTH, BUTTON_HEIGHT)
        .tooltip(Tooltip.create(Component.translatable("combind.ui.removeAlternativeBind")))
        .build();
    }

    @Override
    public void extractContent(GuiGraphicsExtractor extractor, int mouseX, int mouseY, boolean active, float partialTick) {
        int rightEdge = ((KeyBindsListAccessor) list).invokeScrollBarX();
        int y = getContentY() - 2;

        // Position remove button at right edge
        int removeX = rightEdge - REMOVE_BUTTON_WIDTH - RIGHT_MARGIN;
        removeButton.setPosition(removeX, y);
        removeButton.extractRenderState(extractor, mouseX, mouseY, partialTick);

        // Position record button to the left of remove button
        int recordX = removeX - BUTTON_GAP - RECORD_BUTTON_WIDTH;
        recordButton.setPosition(recordX, y);

        if (ComboRecorder.INSTANCE.isRecording() && ComboRecorder.INSTANCE.getTargetBinding() == binding
                && ComboRecorder.INSTANCE.getTargetComboIndex() == comboIndex) {
            recordButton.setMessage(Component.literal("> " + ComboRecorder.INSTANCE.getPreview() + " <"));
        } else {
            recordButton.setMessage(getComboDisplayText());
        }

        recordButton.extractRenderState(extractor, mouseX, mouseY, partialTick);

        // Draw the "*ActionName" label
        extractor.text(
            Minecraft.getInstance().font,
            Component.literal("*").append(actionName),
            getContentX(),
            getContentYMiddle() - Minecraft.getInstance().font.lineHeight / 2,
            0xFFFFFF
        );
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return ImmutableList.of(recordButton, removeButton);
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return ImmutableList.of(recordButton, removeButton);
    }

    @Override
    public void refreshEntry() {
        recordButton.setMessage(getComboDisplayText());
        // Remove button is always active
    }

    private Component getComboDisplayText() {
        List<KeyCombo> combos = binding.getCombos();
        if (comboIndex >= combos.size()) return Component.translatable("key.keyboard.unknown");
        return Component.literal(combos.get(comboIndex).getDisplayName());
    }

    private void startRecording() {
        ComboRecorder.INSTANCE.startRecording(binding, comboIndex);
    }

    private void removeAlternative() {
        binding.removeCombo(comboIndex);
        CombindConfig.save();
        list.resetMappingAndUpdateButtons();
    }
}
