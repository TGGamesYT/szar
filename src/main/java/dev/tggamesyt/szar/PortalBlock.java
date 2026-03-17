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
        if (!(entity instanceof ServerPlayerEntity player)) return;

        long now = world.getTime();
        Long last = cooldowns.get(player.getUuid());
        if (last != null && now - last < 60) return;
        cooldowns.put(player.getUuid(), now);

        TrackerBlockEntity tracker = findTrackerAbove(world, pos);
        if (tracker == null) return;

        MinecraftServer server = world.getServer();
        if (server == null) return;

        // Detect dimension instead of reading isNetherSide
        if (world.getRegistryKey() == World.OVERWORLD) {
            teleportToNether(player, tracker, server, pos);
        } else if (world.getRegistryKey() == Szar.BACKROOMS_KEY) {
            teleportToOverworld(player, tracker, server);
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
    }

    private void teleportToOverworld(ServerPlayerEntity player, TrackerBlockEntity netherTracker,
                                     MinecraftServer server) {
        NbtCompound tag = getOrCreateCustomData(player);
        double returnX = tag.getDouble("EntryX");
        double returnY = tag.getDouble("EntryY");
        double returnZ = tag.getDouble("EntryZ");

        // Read overworld tracker pos
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
        ServerWorld nether = server.getWorld(Szar.BACKROOMS_KEY);

        // Clean up nether side if empty
        if (!netherTracker.hasPlayers() && nether != null) {
            TrackerBlock.restoreAndCleanup(nether,
                    netherTracker.getPos(), netherTracker, server);
        }

        // Clean up overworld tracker too
        if (owTrackerPos != null && overworld != null) {
            if (overworld.getBlockEntity(owTrackerPos) instanceof TrackerBlockEntity owTracker) {
                owTracker.removePlayer(player.getUuid());
                if (!owTracker.hasPlayers()) {
                    TrackerBlock.restoreAndCleanup(overworld, owTrackerPos, owTracker, server);
                }
            }
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
                    return te;
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
}