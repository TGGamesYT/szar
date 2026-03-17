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
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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

        // Only auto-place portal if a player placed this tracker
        if (!tracker.placedByPlayer) return;

        BlockPos portalPos = pos.down(4);

        // Save the original block before replacing it
        tracker.originalPortalBlock = world.getBlockState(portalPos);
        tracker.markDirty();

        world.setBlockState(portalPos, Szar.PORTAL_BLOCK.getDefaultState());
    }
    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        // In survival, only break if holding the special item

        if (!world.isClient && world.getBlockEntity(pos) instanceof TrackerBlockEntity tracker) {
            restoreAndCleanup(world, pos, tracker, world.getServer());
        }
        super.onBreak(world, pos, state, player);
    }

    public static void restoreAndCleanup(World world, BlockPos trackerPos,
                                         TrackerBlockEntity tracker, MinecraftServer server) {
        if (server == null) return;

        // Kick all players tracked here back to overworld
        Set<UUID> players = new HashSet<>(tracker.getPlayersInside());
        for (UUID uuid : players) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(uuid);
            if (player == null) continue;

            PortalBlock.restoreInventory(player, server);

            ServerWorld overworld = server.getWorld(World.OVERWORLD);
            if (overworld != null) {
                NbtCompound tag = PortalDataState.getOrCreate(overworld)
                        .getOrCreatePlayerData(uuid);
                double rx = tag.getDouble("EntryX");
                double ry = tag.getDouble("EntryY");
                double rz = tag.getDouble("EntryZ");
                player.teleport(overworld, rx, ry, rz, player.getYaw(), player.getPitch());
            }
        }

        // Restore original block at portal position
        BlockPos portalPos = trackerPos.down(4);
        if (world.getBlockState(portalPos).getBlock() instanceof PortalBlock) {
            world.setBlockState(portalPos, tracker.originalPortalBlock);
        }

        // Remove the tracker itself
        world.removeBlock(trackerPos, false);
    }
}