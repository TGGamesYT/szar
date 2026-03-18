package dev.tggamesyt.szar;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

public class PortalBlock extends Block {

    // Cooldown tracker so players don't teleport 20x per second
    private static final java.util.Map<java.util.UUID, Long> cooldowns = new java.util.HashMap<>();

    public PortalBlock(Settings settings) {
        super(settings);
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, net.minecraft.world.BlockView world,
                                        BlockPos pos, ShapeContext ctx) {
        return VoxelShapes.empty();
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (world.isClient) return;

        // Cooldown check
        long now = world.getTime();
        Long last = cooldowns.get(entity.getUuid());
        if (last != null && now - last < 60) return;
        cooldowns.put(entity.getUuid(), now);

        TrackerBlockEntity tracker = findTrackerAbove(world, pos);
        if (tracker == null) return;

        MinecraftServer server = world.getServer();
        if (server == null) return;

        if (entity instanceof ServerPlayerEntity player) {
            // Full player handling — inventory save, tracker registration, etc.
            if (world.getRegistryKey() == World.OVERWORLD) {
                teleportToNether(player, tracker, server, pos);
            } else if (world.getRegistryKey() == Szar.BACKROOMS_KEY) {
                teleportToOverworld(player, tracker, server);
            }
        } else {
            // Non-player entity — just teleport, no inventory or tracker registration
            if (world.getRegistryKey() == World.OVERWORLD) {
                ServerWorld backrooms = server.getWorld(Szar.BACKROOMS_KEY);
                if (backrooms == null) return;
                double safeY = findSafeY(backrooms, (int) entity.getX(), (int) entity.getZ());
                entity.teleport(backrooms, entity.getX(), safeY, entity.getZ(),
                        java.util.Set.of(), entity.getYaw(), entity.getPitch());
            } else {
                // Non-player entity — just teleport, no inventory or tracker registration
                if (world.getRegistryKey() == World.OVERWORLD) {
                    ServerWorld backrooms = server.getWorld(Szar.BACKROOMS_KEY);
                    if (backrooms == null) return;
                    double safeY = findSafeY(backrooms, (int) entity.getX(), (int) entity.getZ());
                    entity.teleport(backrooms, entity.getX(), safeY, entity.getZ(),
                            java.util.Set.of(), entity.getYaw(), entity.getPitch());
                } else if (world.getRegistryKey() == Szar.BACKROOMS_KEY) {
                    ServerWorld overworld = server.getWorld(World.OVERWORLD);
                    if (overworld == null) return;

                    double baseX = tracker.returnX;
                    double baseY = tracker.returnY;
                    double baseZ = tracker.returnZ;

                    // If no player has used this portal yet, fallback
                    if (baseX == 0 && baseY == 0 && baseZ == 0) {
                        double safeY = findSafeY(overworld, (int) entity.getX(), (int) entity.getZ());
                        entity.teleport(overworld, entity.getX(), safeY, entity.getZ(),
                                java.util.Set.of(), entity.getYaw(), entity.getPitch());
                        return;
                    }

                    // Search up to 10 blocks offset in XZ and Y for a safe spot not above a portal
                    BlockPos safePos = findSafeSpotNearEntry(overworld, baseX, baseY, baseZ);
                    entity.teleport(overworld, safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5,
                            java.util.Set.of(), entity.getYaw(), entity.getPitch());
                }
            }
        }
    }

    private void teleportToNether(ServerPlayerEntity player, TrackerBlockEntity tracker,
                                  MinecraftServer server, BlockPos portalPos) {
        // Save where this player entered — stored persistently so nether portal can read it
        NbtCompound tag = getOrCreateCustomData(player);
        tag.putDouble("EntryX", player.getX());
        tag.putDouble("EntryY", player.getY() + 6); // a few blocks above so they don't fall back in
        tag.putDouble("EntryZ", player.getZ());
        tag.putInt("OwnerTrackerX", tracker.getPos().getX());
        tag.putInt("OwnerTrackerY", tracker.getPos().getY());
        tag.putInt("OwnerTrackerZ", tracker.getPos().getZ());

        tracker.returnX = player.getX();
        tracker.returnY = player.getY() + 6;
        tracker.returnZ = player.getZ();
        tracker.markDirty();

        // Save inventory
        NbtList savedInventory = saveInventory(player);
        saveInventoryToPlayer(player, savedInventory);

        // Clear inventory
        player.getInventory().clear();

        // Register player in tracker
        tracker.addPlayer(player.getUuid());

        // Teleport
        ServerWorld nether = server.getWorld(Szar.BACKROOMS_KEY);
        if (nether == null) return;

        double netherX = player.getX();
        double netherZ = player.getZ();
        double netherY = findSafeY(nether, (int) netherX, (int) netherZ);

        player.teleport(nether, netherX, netherY, netherZ,
                player.getYaw(), player.getPitch());

        BlockPos destPortalPos = new BlockPos((int) netherX, (int) netherY, (int) netherZ);
        for (int i = 1; i <= 5; i++) {
            BlockPos check = destPortalPos.up(i);
            if (nether.getBlockState(check).getBlock() instanceof TrackerBlock) {
                if (nether.getBlockEntity(check) instanceof TrackerBlockEntity backroomsTracker) {
                    backroomsTracker.returnX = player.getX();
                    backroomsTracker.returnY = player.getY() + 6;
                    backroomsTracker.returnZ = player.getZ();
                    backroomsTracker.markDirty();
                }
                break;
            }
        }
    }

    private void teleportToOverworld(ServerPlayerEntity player, TrackerBlockEntity netherTracker,
                                     MinecraftServer server) {
        NbtCompound tag = getOrCreateCustomData(player);
        double returnX = tag.getDouble("EntryX");
        double returnY = tag.getDouble("EntryY");
        double returnZ = tag.getDouble("EntryZ");

        BlockPos owTrackerPos = null;
        if (tag.contains("OwnerTrackerX")) {
            owTrackerPos = new BlockPos(
                    tag.getInt("OwnerTrackerX"),
                    tag.getInt("OwnerTrackerY"),
                    tag.getInt("OwnerTrackerZ")
            );
        }

        PortalBlock.restoreInventory(player, server);
        netherTracker.removePlayer(player.getUuid());

        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        ServerWorld backrooms = server.getWorld(Szar.BACKROOMS_KEY);

        if (owTrackerPos != null && overworld != null) {
            if (overworld.getBlockEntity(owTrackerPos) instanceof TrackerBlockEntity owTracker) {
                // Resolve to root of the overworld group
                TrackerBlockEntity root = owTracker.getRoot(overworld);
                root.removePlayer(player.getUuid());

                if (!root.hasPlayers()) {
                    // Collect and clean up all connected trackers
                    List<BlockPos> allTrackers = new ArrayList<>();
                    allTrackers.add(root.getPos());
                    for (BlockPos childPortal : root.getControlledPortals()) {
                        allTrackers.add(childPortal.up(4));
                    }

                    for (BlockPos trackerPos : allTrackers) {
                        if (overworld.getBlockEntity(trackerPos)
                                instanceof TrackerBlockEntity te) {
                            TrackerBlock.restoreAndCleanup(overworld, trackerPos, te, server);
                        }
                    }
                }
            }
        }

        // Clean up backrooms tracker too
        if (!netherTracker.hasPlayers() && backrooms != null) {
            TrackerBlock.restoreAndCleanup(backrooms,
                    netherTracker.getPos(), netherTracker, server);
        }

        if (overworld == null) return;
        player.teleport(overworld, returnX, returnY, returnZ,
                player.getYaw(), player.getPitch());
    }

    // --- Helpers ---

    private TrackerBlockEntity findTrackerAbove(World world, BlockPos portalPos) {
        for (int i = 1; i <= 5; i++) {
            BlockPos check = portalPos.up(i);
            if (world.getBlockState(check).getBlock() instanceof TrackerBlock) {
                if (world.getBlockEntity(check) instanceof TrackerBlockEntity te) {
                    // Always delegate to root
                    return te.getRoot(world);
                }
            }
        }
        return null;
    }

    private double findSafeY(ServerWorld world, int x, int z) {
        // For backrooms, search from y=8 downward only
        int startY = world.getRegistryKey() == Szar.BACKROOMS_KEY ? 8 : 100;
        for (int y = startY; y > 1; y--) {
            BlockPos feet = new BlockPos(x, y, z);
            BlockPos head = feet.up();
            BlockPos ground = feet.down();
            if (!world.getBlockState(feet).isSolidBlock(world, feet)
                    && !world.getBlockState(head).isSolidBlock(world, head)
                    && world.getBlockState(ground).isSolidBlock(world, ground)) {
                return y;
            }
        }
        return 5; // fallback to inside the backrooms
    }

    // Inventory persistence via player NBT custom data
    private static final String INV_KEY = "SzarSavedInventory";

    private NbtList saveInventory(ServerPlayerEntity player) {
        NbtList list = new NbtList();
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty()) {
                NbtCompound entry = new NbtCompound();
                entry.putInt("Slot", i);
                stack.writeNbt(entry);
                list.add(entry);
            }
        }
        return list;
    }

    private void saveInventoryToPlayer(ServerPlayerEntity player, NbtList inventory) {
        NbtCompound tag = getOrCreateCustomData(player);
        tag.put(INV_KEY, inventory);
    }

    private void restoreInventoryToPlayer(ServerPlayerEntity player) {
        NbtCompound tag = getOrCreateCustomData(player);
        if (!tag.contains(INV_KEY)) return;

        NbtList list = tag.getList(INV_KEY, 10); // 10 = NbtCompound type
        player.getInventory().clear();

        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            int slot = entry.getInt("Slot");
            ItemStack stack = ItemStack.fromNbt(entry);
            player.getInventory().setStack(slot, stack);
        }

        tag.remove(INV_KEY);
    }

    // Fabric doesn't have a built-in persistent custom data on players in 1.20.1
    // without a mod like FAPI's PersistentStateManager trick.
    // The cleanest approach in vanilla Fabric is to store it in a custom
    // ServerState attached to the overworld.
    private NbtCompound getOrCreateCustomData(ServerPlayerEntity player) {
        // We'll use the player's existing nbt stack — store in a sub-tag
        // This works for the session but won't survive a crash mid-dimension.
        // For a robust solution, use a PersistentState (shown below in PortalDataState.java)
        return PortalDataState.getOrCreate(
                player.getServer().getWorld(World.OVERWORLD)
        ).getOrCreatePlayerData(player.getUuid());
    }

    // Change the signature in PortalBlock.java
    public static void restoreInventory(ServerPlayerEntity player, MinecraftServer server) {
        PortalDataState state = PortalDataState.getOrCreate(
                server.getWorld(World.OVERWORLD)
        );
        NbtCompound tag = state.getOrCreatePlayerData(player.getUuid());

        if (!tag.contains(INV_KEY)) return;

        NbtList list = tag.getList(INV_KEY, 10);

        // Build list of saved stacks
        List<ItemStack> savedStacks = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            NbtCompound entry = list.getCompound(i);
            ItemStack stack = ItemStack.fromNbt(entry);
            if (!stack.isEmpty()) {
                savedStacks.add(stack);
            }
        }

        // Try to insert each saved stack into the current inventory
        PlayerInventory inv = player.getInventory();
        for (ItemStack saved : savedStacks) {
            // insertStack tries to stack with existing items first, then finds empty slot
            boolean inserted = inv.insertStack(saved);
            if (!inserted || !saved.isEmpty()) {
                // Inventory full — drop remainder at player's position
                ItemEntity drop = new ItemEntity(
                        player.getWorld(),
                        player.getX(), player.getY(), player.getZ(),
                        saved.copy()
                );
                drop.setPickupDelay(0);
                player.getWorld().spawnEntity(drop);
            }
        }

        state.removePlayerData(player.getUuid());
    }

    private BlockPos findSafeSpotNearEntry(ServerWorld world, double baseX, double baseY, double baseZ) {
        System.out.println(baseX + ", " +  baseY + ", " +  baseZ);
        int bx = (int) baseX;
        int by = (int) baseY;
        int bz = (int) baseZ;

        // First try exact position
        BlockPos exact = new BlockPos(bx, by, bz);
        if (isSafeAndNotPortal(world, exact)) return exact;

        // Spiral outward in XZ only, keep Y fixed to entry Y
        for (int radius = 1; radius <= 10; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) != radius && Math.abs(dz) != radius) continue;
                    BlockPos candidate = new BlockPos(bx + dx, by, bz + dz);
                    if (isSafeAndNotPortal(world, candidate)) return candidate;
                }
            }
        }

        return exact; // fallback
    }

    private boolean isSafeAndNotPortal(ServerWorld world, BlockPos feet) {
        BlockPos head = feet.up();
        BlockPos ground = feet.down();
        return !world.getBlockState(feet).isSolidBlock(world, feet)
                && !world.getBlockState(head).isSolidBlock(world, head)
                && world.getBlockState(ground).isSolidBlock(world, ground)
                && !(world.getBlockState(ground).getBlock() instanceof PortalBlock);
    }
}