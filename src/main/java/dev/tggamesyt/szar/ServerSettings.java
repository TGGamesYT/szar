package dev.tggamesyt.szar;

import java.util.Map;

public class ServerSettings {

    public static void init() {
        // registerSetting(id, defaultValue)  ← must match client setting ids
        ServerConfig.registerSetting("racist",   true);
        ServerConfig.registerSetting("gambling", true);
        ServerConfig.registerSetting("nsfw",     true);

        // registerPreset(id, Map<settingId, value>)  ← null map = "custom"
        ServerConfig.registerPreset("18+", Map.of(
                "racist",   false,
                "gambling", false,
                "nsfw",     false
        ));
        ServerConfig.registerPreset("17+", Map.of(
                "racist",   false,
                "gambling", false,
                "nsfw",     true
        ));
        ServerConfig.registerPreset("minor", Map.of(
                "racist",   true,
                "gambling", true,
                "nsfw",     true
        ));
        ServerConfig.registerPreset("custom", null);
    }
}