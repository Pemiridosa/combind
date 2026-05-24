package net.pemiridosa.combind.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;

public class BooleanEntry extends ConfigEntry<Boolean> {
    public BooleanEntry(String translationKey, boolean defaultValue) {
        super(translationKey, defaultValue);
    }

    @Override
    public OptionInstance<Boolean> asOption() {
        return OptionInstance.createBoolean(
            getTranslationKey(),
            OptionInstance.cachedConstantTooltip(Component.translatable(getTranslationKey() + ".tooltip")),
            value,
            this::set
        );
    }

    @Override public JsonElement toJson()          { return new JsonPrimitive(value); }
    @Override public void fromJson(JsonElement el) { value = el.getAsBoolean(); }
}
