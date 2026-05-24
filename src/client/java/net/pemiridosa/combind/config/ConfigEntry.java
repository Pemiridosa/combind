package net.pemiridosa.combind.config;

import com.google.gson.JsonElement;
import net.minecraft.client.OptionInstance;

public abstract class ConfigEntry<T> {
    private final String translationKey;
    protected T value;
    private final T defaultValue;

    protected ConfigEntry(String translationKey, T defaultValue) {
        this.translationKey = translationKey;
        this.value          = defaultValue;
        this.defaultValue   = defaultValue;
    }

    public T get()                    { return value; }
    public void set(T value)          { this.value = value; }
    public T getDefault()             { return defaultValue; }
    public String getTranslationKey() { return translationKey; }

    public abstract OptionInstance<?> asOption();
    public abstract JsonElement toJson();
    public abstract void fromJson(JsonElement element);
}
