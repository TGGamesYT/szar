package dev.tggamesyt.szar;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class SmilerSpawnManager {

    // Per player, per type: world time when cooldown started
    private static final Map<UUID, Map<SmilerType, Long>> cooldowns = new HashMap<>();
    // Per player, per type: is there currently a live smiler of this type
    private static final Map<UUID, Set<SmilerType>> activeSmilers = new HashMap<>();

    private static final long COOLDOWN_TICKS = 1200; // 1 minute
    // Random spawn check every 3-8 seconds per player
    private static int spawnCheckTimer = 0;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(SmilerSpawnManager::tick);
    }

    private static void tick(MinecraftServer server) {
        ServerWorld backrooms = server.getWorld(Szar.BACKROOMS_KEY);
        if (backrooms == null) return;

        // Only spawn during blackout
        if (BackroomsLightManager.currentEvent != BackroomsLightManager.GlobalEvent.BLACKOUT) {
            // Clean up any lingering smilers if blackout ended
            return;
        }

        spawnCheckTimer--;
        if (spawnCheckTimer > 0) return;
        spawnCheckTimer = 60 + backrooms.random.nextInt(100); // 3-8 seconds

        for (ServerPlayerEntity player : backrooms.getPlayers()) {
            UUID uuid = player.getUuid();
            long now = backrooms.getTime();

            for (SmilerType type : SmilerType.values()) {
                // Skip if already active
                if (isActive(uuid, type)) continue;

                // Skip if on cooldown
                if (isOnCooldown(uuid, type, now)) continue;

                // 40% chance to actually spawn this type this check
                if (backrooms.random.nextFloat() > 0.4f) continue;

                spawnSmiler(backrooms, player, type);
            }
        }
    }

    private static void spawnSmiler(ServerWorld world, ServerPlayerEntity player, SmilerType type) {
        // Find a spawn position in the backrooms — in a corridor, 15-30 blocks from player
        Vec3d spawnPos = findSpawnPos(world, player);
        if (spawnPos == null) return;

        SmilerEntity entity = new SmilerEntity(Szar.SMILER_ENTITY_TYPE, world);
        entity.smilerType = type;
        entity.setTargetPlayer(player);
        entity.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, 0f, 0f);
        world.spawnEntity(entity);

        markActive(player.getUuid(), type);
    }

    private static Vec3d findSpawnPos(ServerWorld world, PlayerEntity player) {
        // Try to find an open spot 15-30 blocks away from player
        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = world.random.nextDouble() * Math.PI * 2;
            double dist = 15 + world.random.nextDouble() * 15;
            double x = player.getX() + Math.cos(angle) * dist;
            double z = player.getZ() + Math.sin(angle) * dist;
            double y = 6; // backrooms floor level + 1

            BlockPos pos = new BlockPos((int) x, (int) y, (int) z);
            // Check it's open space
            if (world.getBlockState(pos).isAir()
                    && world.getBlockState(pos.up()).isAir()) {
                return new Vec3d(x, y, z);
            }
        }
        return null;
    }

    public static void onSmilerActed(UUID playerUUID, SmilerType type) {
        markInactive(playerUUID, type);
        // Start cooldown
        // We don't have world time here so use system time approximation
        // Actually store it via a flag and check in tick
        cooldowns.computeIfAbsent(playerUUID, k -> new HashMap<>())
                .put(type, System.currentTimeMillis());
    }

    public static void onSmilerDied(UUID playerUUID, SmilerType type) {
        markInactive(playerUUID, type);
        cooldowns.computeIfAbsent(playerUUID, k -> new HashMap<>())
                .put(type, System.currentTimeMillis());
    }

    private static boolean isOnCooldown(UUID uuid, SmilerType type, long worldTime) {
        Map<SmilerType, Long> playerCooldowns = cooldowns.get(uuid);
        if (playerCooldowns == null) return false;
        Long cooldownStart = playerCooldowns.get(type);
        if (cooldownStart == null) return false;
        // Use milliseconds: 1 minute = 60000ms
        return (System.currentTimeMillis() - cooldownStart) < 60000;
    }

    private static boolean isActive(UUID uuid, SmilerType type) {
        Set<SmilerType> active = activeSmilers.get(uuid);
        return active != null && active.contains(type);
    }

    private static void markActive(UUID uuid, SmilerType type) {
        activeSmilers.computeIfAbsent(uuid, k -> new HashSet<>()).add(type);
    }

    private static void markInactive(UUID uuid, SmilerType type) {
        Set<SmilerType> active = activeSmilers.get(uuid);
        if (active != null) active.remove(type);
    }
}