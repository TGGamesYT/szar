package dev.tggamesyt.szar;

import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.block.PlantBlock;
import net.minecraft.block.TallPlantBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

public class CannabisBlock extends PlantBlock implements Fertilizable {

    public CannabisBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        if (!world.isClient && random.nextInt(5) == 0) { // 20% chance
            BlockPos above = pos.up();

            if (world.isAir(above)) {
                world.setBlockState(pos, Szar.TALL_CANNABIS_BLOCK.getDefaultState()
                        .with(TallPlantBlock.HALF, DoubleBlockHalf.LOWER));

                world.setBlockState(above, Szar.TALL_CANNABIS_BLOCK.getDefaultState()
                        .with(TallPlantBlock.HALF, DoubleBlockHalf.UPPER));
            }
        }
    }
    @Override
    public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state, boolean isClient) {
        return world.getBlockState(pos.up()).isAir();
    }

    @Override
    public boolean canGrow(World world, Random random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void grow(ServerWorld world, Random random, BlockPos pos, BlockState state) {
        BlockPos above = pos.up();

        world.setBlockState(pos, Szar.TALL_CANNABIS_BLOCK.getDefaultState()
                .with(TallPlantBlock.HALF, DoubleBlockHalf.LOWER));
        world.setBlockState(above, Szar.TALL_CANNABIS_BLOCK.getDefaultState()
                .with(TallPlantBlock.HALF, DoubleBlockHalf.UPPER));
    }
}
