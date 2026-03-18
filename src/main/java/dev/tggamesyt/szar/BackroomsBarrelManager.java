package dev.tggamesyt.szar;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;

import java.util.*;

public class BackroomsBarrelManager {

    private static final Map<UUID, Double> lastX = new HashMap<>();
    private static final Map<UUID, Double> lastZ = new HashMap<>();
    private static final Map<UUID, Double> walkAccumulator = new HashMap<>();
    private static final Map<UUID, Integer> walkThreshold = new HashMap<>();
    private static final Map<UUID, Set<BlockPos>> trackerBarrels = new HashMap<>();
    private static final Map<UUID, Set<BlockPos>> foodBarrels = new HashMap<>();

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(BackroomsBarrelManager::tick);
    }

    private static void tick(MinecraftServer server) {
        ServerWorld backrooms = server.getWorld(Szar.BACKROOMS_KEY);
        if (backrooms == null) return;

        for (ServerPlayerEntity player : backrooms.getPlayers()) {
            UUID uuid = player.getUuid();

            // --- Walk tracking ---
            double px = player.getX();
            double pz = player.getZ();

            if (lastX.containsKey(uuid)) {
                double dx = px - lastX.get(uuid);
                double dz = pz - lastZ.get(uuid);
                double dist = Math.sqrt(dx * dx + dz * dz);
                walkAccumulator.merge(uuid, dist, Double::sum);
            }
            lastX.put(uuid, px);
            lastZ.put(uuid, pz);

            if (!walkThreshold.containsKey(uuid)) {
                walkThreshold.put(uuid, randomThreshold(backrooms.random));
            }

            // --- Check if tracker barrels need clearing ---
            if (trackerBarrels.containsKey(uuid)) {
                checkAndClearTrackerBarrels(backrooms, uuid);
            }

            // --- Check if food barrels need clearing ---
            if (foodBarrels.containsKey(uuid)) {
                checkAndClearFoodBarrels(backrooms, uuid);
            }

            // --- Hunger check (every 20 ticks) ---
            if (backrooms.getTime() % 20 == 0) {
                HungerManager hunger = player.getHungerManager();
                if (hunger.getFoodLevel() <= 10 && !hasAnyFood(player)) {
                    if (!foodBarrels.containsKey(uuid)) {
                        List<BarrelBlockEntity> nearby = getNearbyBarrels(backrooms, player, 16);
                        if (!nearby.isEmpty()) {
                            Set<BlockPos> positions = new HashSet<>();
                            for (BarrelBlockEntity barrel : nearby) {
                                placeItemInBarrel(barrel, new ItemStack(Szar.CAN_OF_BEANS));
                                positions.add(barrel.getPos().toImmutable());
                            }
                            foodBarrels.put(uuid, positions);
                        }
                    }
                }
            }

            // --- Walk threshold check ---
            double walked = walkAccumulator.getOrDefault(uuid, 0.0);
            if (walked >= walkThreshold.getOrDefault(uuid, 500)) {
                List<BarrelBlockEntity> nearby = getNearbyBarrels(backrooms, player, 16);
                if (!nearby.isEmpty()) {
                    Set<BlockPos> positions = new HashSet<>();
                    for (BarrelBlockEntity barrel : nearby) {
                        placeItemInBarrel(barrel, new ItemStack(Szar.TRACKER_BLOCK_ITEM));
                        positions.add(barrel.getPos().toImmutable());
                    }
                    trackerBarrels.put(uuid, positions);
                }
                walkAccumulator.put(uuid, 0.0);
                walkThreshold.put(uuid, randomThreshold(backrooms.random));
            }
        }

        // Clean up data for players no longer in backrooms
        Set<UUID> activePlayers = new HashSet<>();
        for (ServerPlayerEntity p : backrooms.getPlayers()) activePlayers.add(p.getUuid());
        lastX.keySet().retainAll(activePlayers);
        lastZ.keySet().retainAll(activePlayers);
        walkAccumulator.keySet().retainAll(activePlayers);
        walkThreshold.keySet().retainAll(activePlayers);
        foodBarrels.keySet().retainAll(activePlayers);
        trackerBarrels.keySet().retainAll(activePlayers);
    }

    private static void checkAndClearTrackerBarrels(ServerWorld world, UUID uuid) {
        Set<BlockPos> positions = trackerBarrels.get(uuid);
        if (positions == null) return;

        boolean anyTaken = false;
        for (BlockPos pos : positions) {
            if (world.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
                if (!barrelHasItem(barrel, Szar.TRACKER_BLOCK_ITEM.asItem())) {
                    anyTaken = true;
                    break;
                }
            }
        }

        if (anyTaken) {
            for (BlockPos pos : positions) {
                if (world.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
                    removeItemFromBarrel(barrel, Szar.TRACKER_BLOCK_ITEM.asItem());
                }
            }
            trackerBarrels.remove(uuid);
        }
    }

    private static void checkAndClearFoodBarrels(ServerWorld world, UUID uuid) {
        Set<BlockPos> positions = foodBarrels.get(uuid);
        if (positions == null) return;

        boolean anyTaken = false;
        for (BlockPos pos : positions) {
            if (world.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
                if (!barrelHasItem(barrel, Szar.CAN_OF_BEANS.asItem())) {
                    anyTaken = true;
                    break;
                }
            }
        }

        if (anyTaken) {
            for (BlockPos pos : positions) {
                if (world.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
                    removeItemFromBarrel(barrel, Szar.CAN_OF_BEANS.asItem());
                }
            }
            foodBarrels.remove(uuid);
        }
    }

    private static boolean barrelHasItem(BarrelBlockEntity barrel, Item item) {
        for (int i = 0; i < barrel.size(); i++) {
            if (barrel.getStack(i).isOf(item)) return true;
        }
        return false;
    }

    private static void removeItemFromBarrel(BarrelBlockEntity barrel, Item item) {
        for (int i = 0; i < barrel.size(); i++) {
            if (barrel.getStack(i).isOf(item)) {
                barrel.setStack(i, ItemStack.EMPTY);
                barrel.markDirty();
                return;
            }
        }
    }

    private static boolean hasAnyFood(ServerPlayerEntity player) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && (stack.isFood()
                    || stack.isOf(Szar.CAN_OF_BEANS))) {
                return true;
            }
        }
        return false;
    }

    private static void placeItemInBarrel(BarrelBlockEntity barrel, ItemStack item) {
        for (int i = 0; i < barrel.size(); i++) {
            if (barrel.getStack(i).isEmpty()) {
                barrel.setStack(i, item.copy());
                barrel.markDirty();
                return;
            }
        }
    }

    private static List<BarrelBlockEntity> getNearbyBarrels(ServerWorld world,
                                                            ServerPlayerEntity player,
                                                            int radius) {
        List<BarrelBlockEntity> result = new ArrayList<>();
        Box box = player.getBoundingBox().expand(radius);

        BlockPos min = BlockPos.ofFloored(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.ofFloored(box.maxX, box.maxY, box.maxZ);

        for (BlockPos pos : BlockPos.iterate(min, max)) {
            if (world.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
                result.add(barrel);
            }
        }
        return result;
    }

    private static int randomThreshold(net.minecraft.util.math.random.Random random) {
        return 500 + random.nextInt(501);
    }
}