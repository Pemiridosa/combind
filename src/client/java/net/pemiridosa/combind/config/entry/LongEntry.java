package net.pemiridosa.combind.config.entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;

public class LongEntry extends ConfigEntry<Long> {
    private final long min;
    private final long max;

    public LongEntry(String translationKey, long defaultValue, long min, long max) {
        super(translationKey, defaultValue);
        this.min = min;
        this.max = max;
    }

    @Override
    public OptionInstance<Integer> asOption() {
        return new OptionInstance<>(
            getTranslationKey(),
            OptionInstance.cachedConstantTooltip(Component.translatable(getTranslationKey() + ".tooltip")),
            (caption, val) -> Component.translatable("options.generic_value", caption, val + " ms"),
            new OptionInstance.IntRange((int) min, (int) max),
            (int)(long) value,
            newValue -> set((long)(int) newValue)
        );
    }

    @Override public JsonElement toJson()          { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement el) { value = el.getAsLong(); }
}
