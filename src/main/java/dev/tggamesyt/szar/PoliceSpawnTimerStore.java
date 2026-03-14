package dev.tggamesyt.szar;

import net.minecraft.server.network.ServerPlayerEntity;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PoliceSpawnTimerStore {

    private static final long COOLDOWN_TICKS = 20 * 60 * 10L; // 10 minutes in ticks

    // UUID → last spawn time in world ticks
    private static final Map<UUID, Long> lastSpawnTime = new ConcurrentHashMap<>();

    public static boolean canSpawnForPlayer(ServerPlayerEntity player) {
        long now = player.getWorld().getTime();
        Long last = lastSpawnTime.get(player.getUuid());
        if (last == null) return true;
        return (now - last) >= COOLDOWN_TICKS;
    }

    public static void recordSpawn(ServerPlayerEntity player) {
        lastSpawnTime.put(player.getUuid(), player.getWorld().getTime());
    }

    public static void remove(ServerPlayerEntity player) {
        lastSpawnTime.remove(player.getUuid());
    }
}