package dev.tggamesyt.szar;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import java.util.*;

public class PlayerConfigStore {

    // Map of player UUID → (setting id → value)
    private static final Map<UUID, Map<String, Boolean>> store = new HashMap<>();

    public static void set(ServerPlayerEntity player, Map<String, Boolean> config) {
        store.put(player.getUuid(), config);
    }

    public static boolean get(ServerPlayerEntity player, String settingId) {
        Map<String, Boolean> config = store.get(player.getUuid());
        if (config == null) return false; // default if not synced yet
        return config.getOrDefault(settingId, false);
    }

    public static boolean get(PlayerEntity player, String settingId) {
        Map<String, Boolean> config = store.get(player.getUuid());
        if (config == null) return false; // default if not synced yet
        return config.getOrDefault(settingId, false);
    }

    public static boolean get(UUID uuid, String settingId) {
        Map<String, Boolean> config = store.get(uuid);
        if (config == null) return false;
        return config.getOrDefault(settingId, false);
    }

    public static void remove(ServerPlayerEntity player) {
        store.remove(player.getUuid());
    }

    public static boolean hasSynced(ServerPlayerEntity player) {
        return store.containsKey(player.getUuid());
    }
}
