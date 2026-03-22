package dev.tggamesyt.szar;

import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class TicTacToeBlock extends BlockWithEntity {

    public TicTacToeBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    private static final VoxelShape SHAPE = VoxelShapes.union(
            VoxelShapes.cuboid(0f, 0f, 0f, 1f, 0.75f, 1f)
    );

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world,
                                      BlockPos pos, ShapeContext ctx) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world,
                                        BlockPos pos, ShapeContext ctx) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TicTacToeBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (!(player instanceof ServerPlayerEntity serverPlayer)) return ActionResult.PASS;
        if (!(world.getBlockEntity(pos) instanceof TicTacToeBlockEntity be)) return ActionResult.PASS;

        be.handlePlayerJoin(serverPlayer, pos);
        return ActionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient) return null;
        return type == Szar.TIC_TAC_TOE_ENTITY
                ? (w, pos, s, be) -> TicTacToeBlockEntity.tick(w, pos, s,
                (TicTacToeBlockEntity) be)
                : null;
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof TicTacToeBlockEntity be) {
            be.closeScreenForAll(world.getServer());
            if (be.player1 != null) Szar.tttActivePlayers.remove(be.player1);
            if (be.player2 != null) Szar.tttActivePlayers.remove(be.player2);
        }
        super.onBreak(world, pos, state, player);
    }
}