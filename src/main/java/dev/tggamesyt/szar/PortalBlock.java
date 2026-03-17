package dev.tggamesyt.szar;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.Entity;
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
    public void onEntityCollision(BlockState state, World world, BlockPos pos,
                                  Entity entity) {
        if (world.isClient) return;
        if (!(entity instanceof ServerPlayerEntity player)) return;

        // Cooldown check — 3 seconds
        long now = world.getTime();
        Long last = cooldowns.get(player.getUuid());
        if (last != null && now - last < 60) return;
        cooldowns.put(player.getUuid(), now);

        // Find the TrackerBlock above (within 5 blocks)
        TrackerBlockEntity tracker = findTrackerAbove(world, pos);
        if (tracker == null) return;

        MinecraftServer server = world.getServer();
        if (server == null) return;

        if (!tracker.isNetherSide) {
            // --- OVERWORLD → NETHER ---
            teleportToNether(player, tracker, server, pos);
        } else {
            // --- NETHER → OVERWORLD ---
            teleportToOverworld(player, tracker, server);
        }
    }

    private void teleportToNether(ServerPlayerEntity player, TrackerBlockEntity tracker,
                                  MinecraftServer server, BlockPos portalPos) {
        // Save return position (a few blocks above entry)
        tracker.returnX = player.getX();
        tracker.returnY = player.getY() + 3;
        tracker.returnZ = player.getZ();
        tracker.markDirty();

        // Save inventory
        NbtList savedInventory = saveInventory(player);

        // Clear inventory
        player.getInventory().clear();

        // Register player as inside
        tracker.addPlayer(player.getUuid());

        // Teleport to nether
        ServerWorld nether = server.getWorld(World.NETHER);
        if (nether == null) return;

        double netherX = player.getX();
        double netherZ = player.getZ();
        double netherY = findSafeY(nether, (int) netherX, (int) netherZ);

        // Store saved inventory in player's persistent data
        NbtCompound persistent = player.writeNbt(new NbtCompound());
        // We use a custom data attachment via the player's nbt
        saveInventoryToPlayer(player, savedInventory);

        // Store which overworld tracker owns this player
        NbtCompound tag = getOrCreateCustomData(player);
        tag.putInt("OwnerTrackerX", tracker.getPos().getX());
        tag.putInt("OwnerTrackerY", tracker.getPos().getY());
        tag.putInt("OwnerTrackerZ", tracker.getPos().getZ());

        // Generate nether-side portal structure
        BlockPos netherPortalPos = new BlockPos((int) netherX, (int) netherY, (int) netherZ);
        generateNetherPortal(nether, netherPortalPos, tracker);

        // Teleport
        player.teleport(nether, netherX, netherY + 1, netherZ,
                player.getYaw(), player.getPitch());
    }

    private void teleportToOverworld(ServerPlayerEntity player, TrackerBlockEntity tracker,
                                     MinecraftServer server) {
        // Restore inventory
        restoreInventoryToPlayer(player);

        // Remove from nether tracker
        tracker.removePlayer(player.getUuid());

        // Find overworld paired tracker and remove from that too
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld != null && tracker.pairedTrackerPos != null) {
            if (overworld.getBlockEntity(tracker.pairedTrackerPos)
                    instanceof TrackerBlockEntity owTracker) {
                owTracker.removePlayer(player.getUuid());

                // If no players left, remove both trackers and their portal blocks
                if (!owTracker.hasPlayers()) {
                    removePortalStructure(overworld, tracker.pairedTrackerPos);
                    removePortalStructure((ServerWorld) player.getWorld(), tracker.getPos());
                }
            }
        }

        // Teleport back (a few blocks above entry)
        player.teleport(overworld,
                tracker.returnX, tracker.returnY, tracker.returnZ,
                player.getYaw(), player.getPitch());
    }

    // --- Helpers ---

    private TrackerBlockEntity findTrackerAbove(World world, BlockPos portalPos) {
        for (int i = 1; i <= 5; i++) {
            BlockPos check = portalPos.up(i);
            if (world.getBlockState(check).getBlock() instanceof TrackerBlock) {
                if (world.getBlockEntity(check) instanceof TrackerBlockEntity te) {
                    return te;
                }
            }
        }
        return null;
    }

    private double findSafeY(ServerWorld world, int x, int z) {
        // Search from y=100 downward for solid ground with 2 air blocks above
        for (int y = 100; y > 10; y--) {
            BlockPos feet = new BlockPos(x, y, z);
            BlockPos head = feet.up();
            BlockPos ground = feet.down();
            if (!world.getBlockState(feet).isSolidBlock(world, feet)
                    && !world.getBlockState(head).isSolidBlock(world, head)
                    && world.getBlockState(ground).isSolidBlock(world, ground)) {
                return y;
            }
        }
        return 64; // fallback
    }

    private void generateNetherPortal(ServerWorld nether, BlockPos portalPos,
                                      TrackerBlockEntity overworldTracker) {
        // Place TrackerBlock 4 blocks above portal
        BlockPos trackerPos = portalPos.up(4);
        nether.setBlockState(trackerPos, Szar.TRACKER_BLOCK.getDefaultState());

        if (nether.getBlockEntity(trackerPos) instanceof TrackerBlockEntity netherTracker) {
            netherTracker.isNetherSide = true;
            netherTracker.returnX = overworldTracker.returnX;
            netherTracker.returnY = overworldTracker.returnY;
            netherTracker.returnZ = overworldTracker.returnZ;
            netherTracker.pairedTrackerPos = overworldTracker.getPos();
            overworldTracker.pairedTrackerPos = trackerPos;
            overworldTracker.markDirty();
            netherTracker.markDirty();
        }

        // Place portal block at bottom
        nether.setBlockState(portalPos, Szar.PORTAL_BLOCK.getDefaultState());
    }

    private void removePortalStructure(ServerWorld world, BlockPos trackerPos) {
        // Remove tracker
        world.removeBlock(trackerPos, false);
        // Remove portal block (4 below)
        world.removeBlock(trackerPos.down(4), false);
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
}