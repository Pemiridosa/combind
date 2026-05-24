package net.pemiridosa.combind.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
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
    public void addTo(ConfigCategory category, ConfigEntryBuilder entries) {
        category.addEntry(entries
            .startLongField(Component.translatable(getTranslationKey()), value)
            .setDefaultValue(getDefault())
            .setMin(min)
            .setMax(max)
            .setTooltip(Component.translatable(getTranslationKey() + ".tooltip"))
            .setSaveConsumer(this::set)
            .build());
    }

    @Override public JsonElement toJson()           { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement el)  { value = el.getAsLong(); }
}
