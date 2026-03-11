package dev.tggamesyt.szar.client;

import java.util.Map;

public class ConfigPreset {
    public final String id;
    public final String displayName;
    public final Map<String, Boolean> values; // setting id → value, null means "leave as-is" (custom)

    public ConfigPreset(String id, String displayName, Map<String, Boolean> values) {
        this.id = id;
        this.displayName = displayName;
        this.values = values; // null map = Custom preset
    }

    public boolean isCustom() { return values == null; }
}