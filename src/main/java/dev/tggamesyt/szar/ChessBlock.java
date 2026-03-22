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

public class ChessBlock extends BlockWithEntity {

    private static final VoxelShape SHAPE = VoxelShapes.union(
            VoxelShapes.cuboid(0f, 0f, 0f, 1f, 0.75f, 1f)
    );

    public ChessBlock(Settings settings) { super(settings); }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world,
                                      BlockPos pos, ShapeContext ctx) { return SHAPE; }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world,
                                        BlockPos pos, ShapeContext ctx) { return SHAPE; }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ChessBlockEntity(pos, state);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) return ActionResult.SUCCESS;
        if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;
        if (!(world.getBlockEntity(pos) instanceof ChessBlockEntity be)) return ActionResult.PASS;
        be.handlePlayerJoin(sp, pos);
        return ActionResult.SUCCESS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        if (world.isClient) return null;
        return type == Szar.CHESS_ENTITY
                ? (w, pos, s, be) -> ChessBlockEntity.tick(w, pos, s, (ChessBlockEntity) be)
                : null;
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient && world.getBlockEntity(pos) instanceof ChessBlockEntity be) {
            be.closeScreenForAll(world.getServer());
            if (be.player1 != null) Szar.chessActivePlayers.remove(be.player1);
            if (be.player2 != null) Szar.chessActivePlayers.remove(be.player2);
        }
        super.onBreak(world, pos, state, player);
    }
}
