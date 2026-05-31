package net.pemiridosa.combind.mixin.accessor;

import net.minecraft.client.gui.components.AbstractSelectionList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(AbstractSelectionList.class)
public interface AbstractSelectionListAccessor {
    @Accessor("children")
    List<AbstractSelectionList.Entry<?>> getChildren();

    @Invoker("clearEntries")
    void invokeClearEntries();

    @Invoker("addEntry")
    int invokeAddEntry(AbstractSelectionList.Entry<?> entry, int height);

    @Invoker("scrollBarX")
    int invokeScrollBarX();
}
