package dev.tggamesyt.szar;

import com.google.gson.*;
import net.fabricmc.loader.api.FabricLoader;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class ServerConfig {

    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("szar/config.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // ── Registry ──────────────────────────────────────────────────────────────
    private static final LinkedHashMap<String, Boolean> VALUES   = new LinkedHashMap<>();
    private static final LinkedHashMap<String, Boolean> DEFAULTS = new LinkedHashMap<>();
    private static final LinkedHashMap<String, Map<String, Boolean>> PRESETS = new LinkedHashMap<>();
    private static String activePreset = "minor";

    // ── Registration (call from ServerSettings.init()) ────────────────────────

    public static void registerSetting(String id, boolean defaultValue) {
        DEFAULTS.put(id, defaultValue);
        VALUES.put(id, defaultValue);
    }

    public static void registerPreset(String id, Map<String, Boolean> values) {
        PRESETS.put(id, values == null ? null : new LinkedHashMap<>(values));
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public static boolean get(String id) {
        return VALUES.getOrDefault(id, true);
    }

    public static void set(String id, boolean value) {
        if (!VALUES.containsKey(id)) throw new IllegalArgumentException("Unknown setting: " + id);
        VALUES.put(id, value);
        // If this doesn't match any preset anymore, switch to custom
        activePreset = detectPreset();
    }

    public static String getActivePreset() { return activePreset; }

    public static Set<String> getSettingIds() { return VALUES.keySet(); }

    public static Map<String, Boolean> snapshot() { return Collections.unmodifiableMap(VALUES); }

    public static boolean hasPreset(String id) { return PRESETS.containsKey(id); }

    public static Set<String> getPresetIds() { return PRESETS.keySet(); }

    /** Applies a preset by id. Returns false if unknown. */
    public static boolean applyPreset(String presetId) {
        Map<String, Boolean> preset = PRESETS.get(presetId);
        if (preset == null && !PRESETS.containsKey(presetId)) return false; // unknown
        activePreset = presetId;
        if (preset != null) { // null = "custom", don't touch values
            preset.forEach((id, val) -> {
                if (VALUES.containsKey(id)) VALUES.put(id, val);
            });
        }
        return true;
    }

    private static String detectPreset() {
        for (Map.Entry<String, Map<String, Boolean>> entry : PRESETS.entrySet()) {
            Map<String, Boolean> pValues = entry.getValue();
            if (pValues == null) continue; // skip custom
            if (pValues.equals(VALUES)) return entry.getKey();
        }
        return "custom";
    }

    // ── Save / Load ───────────────────────────────────────────────────────────

    public static void save() {
        JsonObject root = loadRawRoot();

        JsonObject server = new JsonObject();
        server.addProperty("preset", activePreset);
        JsonObject vals = new JsonObject();
        VALUES.forEach(vals::addProperty);
        server.add("values", vals);
        root.add("server", server);

        CONFIG_PATH.getParent().toFile().mkdirs();
        try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(root, w);
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static void load() {
        JsonObject root = loadRawRoot();
        if (!root.has("server")) { save(); return; }

        JsonObject server = root.getAsJsonObject("server");
        if (server.has("preset")) activePreset = server.get("preset").getAsString();
        if (server.has("values")) {
            JsonObject vals = server.getAsJsonObject("values");
            VALUES.forEach((id, def) -> {
                if (vals.has(id)) VALUES.put(id, vals.get(id).getAsBoolean());
            });
        }
    }

    private static JsonObject loadRawRoot() {
        if (!Files.exists(CONFIG_PATH)) return new JsonObject();
        try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
            JsonObject obj = GSON.fromJson(r, JsonObject.class);
            return obj != null ? obj : new JsonObject();
        } catch (IOException e) { return new JsonObject(); }
    }
}