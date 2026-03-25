package dev.tggamesyt.szar;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class BlueprintStairsBlock extends StairsBlock implements BlockEntityProvider {

    public BlueprintStairsBlock(Settings settings) {
        super(Blocks.STONE.getDefaultState(), settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new BlueprintBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        return BlueprintBehavior.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    public float calcBlockBreakingDelta(BlockState state, PlayerEntity player,
                                        BlockView world, BlockPos pos) {
        return BlueprintBehavior.calcBreakingDelta(state, player, world, pos,
                super.calcBlockBreakingDelta(state, player, world, pos));
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        BlueprintBehavior.onBreak(world, pos, state, player);
        super.onBreak(world, pos, state, player);
    }
}