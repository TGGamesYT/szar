package dev.tggamesyt.szar;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TrackerBlock extends Block implements BlockEntityProvider {

    public TrackerBlock(Settings settings) {
        super(settings);
    }

    // No hitbox
    // 1x1x1 pixel cube — invisible in practice but gives particles a valid bounding box
    private static final VoxelShape TINY = Block.createCuboidShape(7.9, 7.9, 7.9, 8.1, 8.1, 8.1);

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world,
                                      BlockPos pos, ShapeContext ctx) {
        if (ctx instanceof EntityShapeContext esc && esc.getEntity() instanceof PlayerEntity player) {
            if (isHoldingTracker(player)) {
                return VoxelShapes.fullCube();
            }
        }
        // Too small to see or select, but has valid bounds so particle manager doesn't crash
        return TINY;
    }


    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world,
                                        BlockPos pos, ShapeContext ctx) {
        return VoxelShapes.empty(); // never has collision, only outline
    }

    @Override
    public VoxelShape getRaycastShape(BlockState state, BlockView world, BlockPos pos) {
        // This is what allows the player to actually target and break it
        return VoxelShapes.fullCube();
    }

    private boolean isHoldingTracker(PlayerEntity player) {
        return player.getMainHandStack().isOf(Szar.TRACKER_BLOCK_ITEM.asItem())
                || player.getOffHandStack().isOf(Szar.TRACKER_BLOCK_ITEM.asItem());
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TrackerBlockEntity(pos, state);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state,
                         @Nullable LivingEntity placer, ItemStack itemStack) {
        if (world.isClient) return;
        if (!(world.getBlockEntity(pos) instanceof TrackerBlockEntity tracker)) return;
        if (!tracker.placedByPlayer) return;

        // Check all 6 neighbors for an existing tracker
        TrackerBlockEntity parentTracker = findNeighborTracker(world, pos);

        BlockPos portalPos = pos.down(4);
        BlockState originalBlock = world.getBlockState(portalPos);

        if (parentTracker != null) {
            // Merge — this becomes a child
            tracker.parentTrackerPos = parentTracker.getPos();
            tracker.originalPortalBlock = originalBlock;
            tracker.markDirty();

            // Register portal with parent
            parentTracker.addPortal(portalPos);

            // Place portal block
            world.setBlockState(portalPos, Szar.PORTAL_BLOCK.getDefaultState());
        } else {
            // Standalone — normal behavior
            tracker.originalPortalBlock = originalBlock;
            tracker.markDirty();
            world.setBlockState(portalPos, Szar.PORTAL_BLOCK.getDefaultState());
        }
    }

    @Nullable
    private TrackerBlockEntity findNeighborTracker(World world, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.offset(dir);
            if (world.getBlockState(neighbor).getBlock() instanceof TrackerBlock) {
                if (world.getBlockEntity(neighbor) instanceof TrackerBlockEntity te) {
                    // Always return the root of the neighbor
                    return te.getRoot(world);
                }
            }
        }
        return null;
    }
    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof TrackerBlockEntity tracker) {
            // Resolve to root
            TrackerBlockEntity root = tracker.getRoot(world);
            BlockPos rootPos = root.getPos();

            // Collect all connected tracker positions (root + all children)
            List<BlockPos> allTrackers = new ArrayList<>();
            allTrackers.add(rootPos);
            for (BlockPos childPortal : root.getControlledPortals()) {
                allTrackers.add(childPortal.up(4));
            }

            // Clean up each tracker and its portal
            for (BlockPos trackerPos : allTrackers) {
                if (world.getBlockEntity(trackerPos) instanceof TrackerBlockEntity te) {
                    cleanupPortalOnly(world, trackerPos, te, world.getServer());
                    world.removeBlock(trackerPos, false);
                }
            }
        }
        super.onBreak(world, pos, state, player);
    }

    public static void restoreAndCleanup(World world, BlockPos trackerPos,
                                         TrackerBlockEntity tracker, MinecraftServer server) {
        if (server == null) return;

        Set<UUID> players = new HashSet<>(tracker.getPlayersInside());
        for (UUID uuid : players) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player == null) continue;

            ServerWorld overworld = server.getWorld(World.OVERWORLD);
            if (overworld == null) continue;

            // Read coords BEFORE restoreInventory wipes the state
            NbtCompound tag = PortalDataState.getOrCreate(overworld)
                    .getOrCreatePlayerData(uuid);
            double rx = tag.getDouble("EntryX");
            double ry = tag.getDouble("EntryY");
            double rz = tag.getDouble("EntryZ");

            PortalBlock.restoreInventory(player, server);

            player.teleport(overworld, rx, ry, rz, player.getYaw(), player.getPitch());
        }

        BlockPos portalPos = trackerPos.down(4);
        if (world.getBlockState(portalPos).getBlock() instanceof PortalBlock) {
            world.setBlockState(portalPos, tracker.originalPortalBlock);
        }

        world.removeBlock(trackerPos, false);
    }
    public static void cleanupPortalOnly(World world, BlockPos trackerPos,
                                         TrackerBlockEntity tracker, MinecraftServer server) {
        if (server == null) return;

        Set<UUID> players = new HashSet<>(tracker.getPlayersInside());
        for (UUID uuid : players) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player == null) continue;

            ServerWorld overworld = server.getWorld(World.OVERWORLD);
            if (overworld == null) continue;

            // Read coords BEFORE restoreInventory wipes the state
            NbtCompound tag = PortalDataState.getOrCreate(overworld)
                    .getOrCreatePlayerData(uuid);
            double rx = tag.getDouble("EntryX");
            double ry = tag.getDouble("EntryY");
            double rz = tag.getDouble("EntryZ");

            PortalBlock.restoreInventory(player, server);

            player.teleport(overworld, rx, ry, rz, player.getYaw(), player.getPitch());
        }

        BlockPos portalPos = trackerPos.down(4);
        if (world.getBlockState(portalPos).getBlock() instanceof PortalBlock) {
            world.setBlockState(portalPos, tracker.originalPortalBlock);
        }
    }
}