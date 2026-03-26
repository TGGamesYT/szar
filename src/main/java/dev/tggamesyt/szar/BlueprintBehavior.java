package dev.tggamesyt.szar;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class BlueprintBehavior {

    /**
     * Call from your block's onUse().
     * Right click with a block → stores it, consumes one from stack.
     * Right click with empty hand → clears stored block.
     */
    public static ActionResult onUse(BlockState state, World world, BlockPos pos,
                                     PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;

        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof BlueprintBlockEntity blueprint)) return ActionResult.PASS;

        ItemStack held = player.getStackInHand(hand);

        if (held.isEmpty() && player.isSneaking()) {
            blueprint.clearStoredBlock();
            return ActionResult.SUCCESS;
        }

        if (!held.isEmpty() && held.getItem() instanceof BlockItem blockItem && !blueprint.hasStoredBlock()) {
            if (blockItem.getBlock() instanceof BlueprintStairsBlock ||
                    blockItem.getBlock() instanceof BlueprintSlabBlock ||
                    blockItem.getBlock() instanceof BlueprintDoorBlock ||
                    blockItem.getBlock() instanceof BlueprintTrapDoorBlock ||
                    blockItem.getBlock() instanceof BlueprintWallBlock ||
                    blockItem.getBlock() instanceof BlueprintFenceBlock) {
                return ActionResult.PASS;
            }
            String id = Registries.BLOCK.getId(blockItem.getBlock()).toString();
            blueprint.setStoredBlock(id);
            if (!player.isCreative()) held.decrement(1);
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    /**
     * Call from your block's calcBlockBreakingDelta() to apply stored hardness.
     */
    public static float calcBreakingDelta(BlockState state, PlayerEntity player,
                                          BlockView world, BlockPos pos, float baseHardness) {
        BlockEntity be = world.getBlockEntity(pos);

        if (!(be instanceof BlueprintBlockEntity blueprint) || !blueprint.hasStoredBlock()) {
            return baseHardness;
        }

        BlockState storedState = Registries.BLOCK
                .get(new Identifier(blueprint.getStoredBlockId()))
                .getDefaultState();

        // Fully delegate to vanilla logic
        return storedState.calcBlockBreakingDelta(player, world, pos);
    }

    /**
     * Call from your block's onBreak() to drop the stored block instead.
     */
    public static void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        BlockEntity be = world.getBlockEntity(pos);

        if (!(be instanceof BlueprintBlockEntity blueprint) || !blueprint.hasStoredBlock()) {
            return;
        }

        // Drop stored block
        if (!world.isClient) {
            Block.dropStack(world, pos, blueprint.getStoredDrop());
        }

        // Handle doors (break other half properly)
        if (state.getBlock() instanceof DoorBlock) {
            DoubleBlockHalf half = state.get(DoorBlock.HALF);
            BlockPos otherPos = (half == DoubleBlockHalf.LOWER) ? pos.up() : pos.down();

            BlockState otherState = world.getBlockState(otherPos);
            BlockEntity otherBe = world.getBlockEntity(otherPos);

            if (otherBe instanceof BlueprintBlockEntity otherBlueprint && otherBlueprint.hasStoredBlock()) {
                if (!world.isClient) {
                    Block.dropStack(world, otherPos, otherBlueprint.getStoredDrop());
                }
            }
        }
    }

    private static void dropStack(World world, BlockPos pos, ItemStack stack) {
        net.minecraft.entity.ItemEntity entity = new net.minecraft.entity.ItemEntity(
                world,
                pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                stack
        );
        world.spawnEntity(entity);
    }
}