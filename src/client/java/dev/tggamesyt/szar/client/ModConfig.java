package dev.tggamesyt.szar.client;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ModConfig {

    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("szar/config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ── Registry ──────────────────────────────────────────────────────────────
    private static final LinkedHashMap<String, ConfigEntry>  SETTINGS = new LinkedHashMap<>();
    private static final LinkedHashMap<String, ConfigPreset> PRESETS  = new LinkedHashMap<>();
    private static String activePresetId = null;

    // ── Registration API ──────────────────────────────────────────────────────

    /**
     * Register a toggle setting.
     * @param id              Unique ID, used in save file and code
     * @param displayName     Label shown in the config screen
     * @param defaultValue    Default on/off state
     * @param resourcePackId  Resource pack to enable/disable with this setting, or null
     */
    public static ConfigEntry newSetting(String id, String displayName,
                                         boolean defaultValue, String resourcePackId) {
        ConfigEntry entry = new ConfigEntry(id, displayName, defaultValue, resourcePackId);
        SETTINGS.put(id, entry);
        return entry;
    }

    /** Register a toggle setting with no linked resource pack. */
    public static ConfigEntry newSetting(String id, String displayName, boolean defaultValue) {
        return newSetting(id, displayName, defaultValue, null);
    }

    /**
     * Register a preset.
     * @param id          Unique ID
     * @param displayName Label shown on the button
     * @param values      Map of setting id → value. Pass null to make this the "Custom" preset.
     */
    public static ConfigPreset newPreset(String id, String displayName, Map<String, Boolean> values) {
        ConfigPreset preset = new ConfigPreset(id, displayName, values);
        PRESETS.put(id, preset);
        if (activePresetId == null) activePresetId = id; // first preset is default
        return preset;
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public static boolean get(String settingId) {
        ConfigEntry e = SETTINGS.get(settingId);
        if (e == null) throw new IllegalArgumentException("Unknown setting: " + settingId);
        return e.get();
    }

    public static Collection<ConfigEntry>  allSettings() { return SETTINGS.values(); }
    public static Collection<ConfigPreset> allPresets()  { return PRESETS.values(); }

    public static String getActivePresetId() { return activePresetId; }

    public static void applyPreset(String presetId) {
        ConfigPreset preset = PRESETS.get(presetId);
        if (preset == null) throw new IllegalArgumentException("Unknown preset: " + presetId);
        activePresetId = presetId;
        if (!preset.isCustom()) {
            preset.values.forEach((id, val) -> {
                ConfigEntry e = SETTINGS.get(id);
                if (e != null) e.set(val);
            });
        }
        // Custom preset: leave all values as-is
    }

    /** Call this when the user manually changes a toggle — auto-switches to the custom preset. */
    public static void setAndMarkCustom(String settingId, boolean value) {
        ConfigEntry e = SETTINGS.get(settingId);
        if (e == null) throw new IllegalArgumentException("Unknown setting: " + settingId);
        e.set(value);
        // Find custom preset and switch to it
        PRESETS.values().stream()
                .filter(ConfigPreset::isCustom)
                .findFirst()
                .ifPresent(p -> activePresetId = p.id);
    }

    // ── Save / Load ───────────────────────────────────────────────────────────

    public static void save() {
        JsonObject root = new JsonObject();
        root.addProperty("preset", activePresetId);
        JsonObject values = new JsonObject();
        SETTINGS.forEach((id, entry) -> values.addProperty(id, entry.get()));
        root.add("values", values);
        try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(root, w);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) { save(); return; }
        try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject root = GSON.fromJson(r, JsonObject.class);
            if (root == null) return;
            if (root.has("preset")) activePresetId = root.get("preset").getAsString();
            if (root.has("values")) {
                JsonObject values = root.getAsJsonObject("values");
                SETTINGS.forEach((id, entry) -> {
                    if (values.has(id)) entry.set(values.get(id).getAsBoolean());
                });
            }
        } catch (IOException e) { e.printStackTrace(); }
    }
}