package net.pemiridosa.combind.mixin.accessor;

import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(KeyBindsList.class)
public interface KeyBindsListAccessor {
    @Invoker("scrollBarX")
    int invokeScrollBarX();
}
