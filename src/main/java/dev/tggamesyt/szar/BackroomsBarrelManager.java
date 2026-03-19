package dev.tggamesyt.szar;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.entity.BarrelBlockEntity;
import net.minecraft.entity.ItemEntity;
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

    // Cooldown: world time when food was last taken from a barrel per player
    private static final Map<UUID, Long> foodTakenCooldown = new HashMap<>();

    // How far from ANY player a barrel must be to get cleared
    private static final int CLEAR_RANGE = 32;
    // Cooldown in ticks (20 ticks/sec * 60 sec = 1200)
    private static final long FOOD_COOLDOWN_TICKS = 1200;
    // Range to check for dropped food items on ground
    private static final int DROPPED_FOOD_RANGE = 8;

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(BackroomsBarrelManager::tick);
    }

    private static void tick(MinecraftServer server) {
        ServerWorld backrooms = server.getWorld(Szar.BACKROOMS_KEY);
        if (backrooms == null) return;

        List<ServerPlayerEntity> players = backrooms.getPlayers();

        // --- Clear barrels too far from any player ---
        clearDistantBarrels(backrooms, players, trackerBarrels, Szar.TRACKER_BLOCK_ITEM.asItem());
        clearDistantBarrels(backrooms, players, foodBarrels, null);

        for (ServerPlayerEntity player : players) {
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
                checkAndClearBarrels(backrooms, uuid, trackerBarrels,
                        Szar.TRACKER_BLOCK_ITEM.asItem());
            }

            // --- Check if food barrels need clearing ---
            if (foodBarrels.containsKey(uuid)) {
                boolean anyTaken = checkFoodBarrelsTaken(backrooms, uuid);
                if (anyTaken) {
                    // Clear all food from tracked barrels and start cooldown
                    clearAllFoodBarrels(backrooms, uuid);
                    foodTakenCooldown.put(uuid, backrooms.getTime());
                    foodBarrels.remove(uuid);
                }
            }

            // --- Hunger check (every 20 ticks) ---
            if (backrooms.getTime() % 20 == 0) {
                HungerManager hunger = player.getHungerManager();
                boolean isHungry = hunger.getFoodLevel() <= 10;
                boolean hasFood = hasAnyFood(player);
                boolean hasFoodOnGround = hasFoodDroppedNearby(backrooms, player);
                long lastTaken = foodTakenCooldown.getOrDefault(uuid, -FOOD_COOLDOWN_TICKS);
                boolean onCooldown = (backrooms.getTime() - lastTaken) < FOOD_COOLDOWN_TICKS;

                if (isHungry && !hasFood && !hasFoodOnGround && !onCooldown) {
                    // Ensure ALL nearby barrels have food, not just new ones
                    List<BarrelBlockEntity> nearby = getNearbyBarrels(backrooms, player, 16);
                    Set<BlockPos> positions = foodBarrels.getOrDefault(uuid, new HashSet<>());

                    for (BarrelBlockEntity barrel : nearby) {
                        BlockPos bpos = barrel.getPos().toImmutable();
                        // If this barrel doesn't have food yet, add it
                        if (!barrelHasItem(barrel, Szar.CAN_OF_BEANS.asItem())
                                && !barrelHasItem(barrel, Szar.ALMOND_WATER.asItem())) {
                            Item foodItem = backrooms.random.nextBoolean()
                                    ? Szar.CAN_OF_BEANS.asItem()
                                    : Szar.ALMOND_WATER.asItem();
                            placeItemInBarrel(barrel, new ItemStack(foodItem));
                        }
                        positions.add(bpos);
                    }

                    // Also clear positions that are now out of range
                    positions.removeIf(bpos -> {
                        double d = player.squaredDistanceTo(
                                bpos.getX(), bpos.getY(), bpos.getZ());
                        if (d > 16 * 16) {
                            if (backrooms.getBlockEntity(bpos)
                                    instanceof BarrelBlockEntity b) {
                                removeItemFromBarrel(b, Szar.CAN_OF_BEANS.asItem());
                                removeItemFromBarrel(b, Szar.ALMOND_WATER.asItem());
                            }
                            return true;
                        }
                        return false;
                    });

                    if (!positions.isEmpty()) {
                        foodBarrels.put(uuid, positions);
                    }
                } else if (onCooldown || hasFood || hasFoodOnGround) {
                    // If player now has food or is on cooldown, clear all food barrels
                    if (foodBarrels.containsKey(uuid)) {
                        clearAllFoodBarrels(backrooms, uuid);
                        foodBarrels.remove(uuid);
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
        for (ServerPlayerEntity p : players) activePlayers.add(p.getUuid());
        lastX.keySet().retainAll(activePlayers);
        lastZ.keySet().retainAll(activePlayers);
        walkAccumulator.keySet().retainAll(activePlayers);
        walkThreshold.keySet().retainAll(activePlayers);
        foodBarrels.keySet().retainAll(activePlayers);
        trackerBarrels.keySet().retainAll(activePlayers);
        foodTakenCooldown.keySet().retainAll(activePlayers);
    }

    private static boolean checkFoodBarrelsTaken(ServerWorld world, UUID uuid) {
        Set<BlockPos> positions = foodBarrels.get(uuid);
        if (positions == null) return false;
        for (BlockPos pos : positions) {
            if (world.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
                if (!barrelHasItem(barrel, Szar.CAN_OF_BEANS.asItem())
                        && !barrelHasItem(barrel, Szar.ALMOND_WATER.asItem())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void clearAllFoodBarrels(ServerWorld world, UUID uuid) {
        Set<BlockPos> positions = foodBarrels.get(uuid);
        if (positions == null) return;
        for (BlockPos pos : positions) {
            if (world.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
                removeItemFromBarrel(barrel, Szar.CAN_OF_BEANS.asItem());
                removeItemFromBarrel(barrel, Szar.ALMOND_WATER.asItem());
            }
        }
    }

    private static boolean hasFoodDroppedNearby(ServerWorld world, ServerPlayerEntity player) {
        Box box = player.getBoundingBox().expand(DROPPED_FOOD_RANGE);
        List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, box, e -> {
            ItemStack stack = e.getStack();
            return stack.isFood()
                    || stack.isOf(Szar.CAN_OF_BEANS)
                    || stack.isOf(Szar.ALMOND_WATER);
        });
        return !items.isEmpty();
    }

    private static void clearDistantBarrels(ServerWorld world,
                                            List<ServerPlayerEntity> players,
                                            Map<UUID, Set<BlockPos>> barrelMap,
                                            Item item) {
        Iterator<Map.Entry<UUID, Set<BlockPos>>> iter = barrelMap.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<UUID, Set<BlockPos>> entry = iter.next();
            Set<BlockPos> positions = entry.getValue();

            boolean allDistant = true;
            for (BlockPos pos : positions) {
                for (ServerPlayerEntity player : players) {
                    if (player.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ())
                            <= CLEAR_RANGE * CLEAR_RANGE) {
                        allDistant = false;
                        break;
                    }
                }
                if (!allDistant) break;
            }

            if (allDistant) {
                for (BlockPos pos : positions) {
                    if (world.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
                        if (item != null) {
                            removeItemFromBarrel(barrel, item);
                        } else {
                            removeItemFromBarrel(barrel, Szar.CAN_OF_BEANS.asItem());
                            removeItemFromBarrel(barrel, Szar.ALMOND_WATER.asItem());
                        }
                    }
                }
                iter.remove();
            }
        }
    }

    private static void checkAndClearBarrels(ServerWorld world, UUID uuid,
                                             Map<UUID, Set<BlockPos>> barrelMap, Item item) {
        Set<BlockPos> positions = barrelMap.get(uuid);
        if (positions == null) return;

        boolean anyTaken = false;
        for (BlockPos pos : positions) {
            if (world.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
                if (!barrelHasItem(barrel, item)) {
                    anyTaken = true;
                    break;
                }
            }
        }

        if (anyTaken) {
            for (BlockPos pos : positions) {
                if (world.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
                    removeItemFromBarrel(barrel, item);
                }
            }
            barrelMap.remove(uuid);
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
                    || stack.isOf(Szar.CAN_OF_BEANS)
                    || stack.isOf(Szar.ALMOND_WATER))) {
                return true;
            }
        }
        return false;
    }

    private static void placeItemInBarrel(BarrelBlockEntity barrel, ItemStack item) {
        List<Integer> emptySlots = new ArrayList<>();
        for (int i = 0; i < barrel.size(); i++) {
            if (barrel.getStack(i).isEmpty()) emptySlots.add(i);
        }
        if (emptySlots.isEmpty()) return;
        int slot = emptySlots.get((int)(Math.random() * emptySlots.size()));
        barrel.setStack(slot, item.copy());
        barrel.markDirty();
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