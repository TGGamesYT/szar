package dev.tggamesyt.szar;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

public class FaszBlock extends Block {

    public FaszBlock() {
        super(Settings.copy(Blocks.STONE));
    }

    private static final VoxelShape SHAPE_1 = Block.createCuboidShape(
            6, 0, 2,
            10, 4, 6
    );

    // Element 2: from [6, 0, 10] to [10, 4, 14]
    private static final VoxelShape SHAPE_2 = Block.createCuboidShape(
            6, 0, 10,
            10, 4, 14
    );

    // Element 3: from [6, 4, 6] to [10, 32, 10]
    private static final VoxelShape SHAPE_3 = Block.createCuboidShape(
            6, 4, 6,
            10, 32, 10
    );

    // Combine all shapes
    private static final VoxelShape SHAPE = VoxelShapes.union(
            SHAPE_1,
            SHAPE_2,
            SHAPE_3
    );


    @Override
    public VoxelShape getOutlineShape(
            BlockState state,
            BlockView world,
            BlockPos pos,
            ShapeContext context
    ) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(
            BlockState state,
            BlockView world,
            BlockPos pos,
            ShapeContext context
    ) {
        // behave like vanilla: collision follows outline
        return SHAPE;
    }
}
