package net.pemiridosa.combind.config;

import net.pemiridosa.combind.impl.CombindConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;

public class CombindConfigScreen extends OptionsSubScreen {
    public CombindConfigScreen(Screen parent) {
        super(parent, Minecraft.getInstance().options, Component.translatable("combind.config.title"));
    }

    @Override
    protected void addOptions() {
        if (this.list != null)
            this.list.addSmall(CombindConfig.config.asOptions());
    }

    @Override
    public void removed() {
        CombindConfig.save();
    }
}
