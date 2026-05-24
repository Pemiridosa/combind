package net.pemiridosa.combind.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.network.chat.Component;

public class BooleanEntry extends ConfigEntry<Boolean> {
    public BooleanEntry(String translationKey, boolean defaultValue) {
        super(translationKey, defaultValue);
    }

    @Override
    public void addTo(ConfigCategory category, ConfigEntryBuilder entries) {
        category.addEntry(entries
            .startBooleanToggle(Component.translatable(getTranslationKey()), value)
            .setDefaultValue(getDefault())
            .setTooltip(Component.translatable(getTranslationKey() + ".tooltip"))
            .setSaveConsumer(this::set)
            .build());
    }

    @Override public JsonElement toJson() {
        return new JsonPrimitive(value);
    }

    @Override public void fromJson(JsonElement el) {
        value = el.getAsBoolean();
    }
}
