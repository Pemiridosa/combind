package net.pemiridosa.combind.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.pemiridosa.combind.impl.CombindConfig;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class CombindConfigScreen {
    private CombindConfigScreen() {}

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
            .setParentScreen(parent)
            .setTitle(Component.translatable("combind.config.title"))
            .setSavingRunnable(CombindConfig::save);

        CombindConfig.config.addEntries(
            builder.getOrCreateCategory(Component.translatable("combind.config.category.general")),
            builder.entryBuilder()
        );

        return builder.build();
    }
}
