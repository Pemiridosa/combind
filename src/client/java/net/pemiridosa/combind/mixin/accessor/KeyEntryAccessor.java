package net.pemiridosa.combind.mixin.accessor;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyBindsList.KeyEntry.class)
public interface KeyEntryAccessor {
    @Accessor("key")
    KeyMapping getKey();

    @Accessor("name")
    Component getName();
}
