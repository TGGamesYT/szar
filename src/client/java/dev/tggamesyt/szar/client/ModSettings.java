package dev.tggamesyt.szar.client;

import java.util.Map;

public class ModSettings {
    // ── Declare setting references for easy typed access ──────────────────────
    public static ConfigEntry RACIST;
    public static ConfigEntry GAMBLING;
    public static ConfigEntry NSFW;
    // To add a new setting: just add a line here and in init() below. That's it.

    public static void init() {
        // newSetting(id, displayName, defaultValue)
        // newSetting(id, displayName, defaultValue, "resourcepack/id")
        RACIST   = ModConfig.newSetting("racist",    "Block Racist content",      true, "szar:racist");
        GAMBLING      = ModConfig.newSetting("gambling",       "Block Gambling",         true);
        NSFW = ModConfig.newSetting("nsfw",  "Block NSFW content",true, "szar:nsfw");

        // ── Presets ───────────────────────────────────────────────────────────
        // newPreset(id, displayName, Map<settingId, value>)
        // Pass null map for the "custom" preset (user-editable, no fixed values)

        ModConfig.newPreset("none", "18+", Map.of(
                "racist",   false,
                "gambling",      false,
                "nsfw", false
        ));
        ModConfig.newPreset("some", "17+", Map.of(
                "racist",   false,
                "gambling",      false,
                "nsfw", true
        ));
        ModConfig.newPreset("all", "Minor", Map.of(
                "racist",   true,
                "gambling",      true,
                "nsfw", true
        ));
        ModConfig.newPreset("custom", "Custom", null); // null = custom, toggles stay editable
    }
}