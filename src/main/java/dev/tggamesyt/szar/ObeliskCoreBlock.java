package dev.tggamesyt.szar;

import net.minecraft.block.Block;
import net.minecraft.block.BlockEntityProvider;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;
import org.jetbrains.annotations.Nullable;

public class ObeliskCoreBlock extends Block implements BlockEntityProvider {

    public ObeliskCoreBlock(Settings settings) {
        super(settings);
    }

    @Nullable
    @Override
    public ObeliskCoreBlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ObeliskCoreBlockEntity(pos, state);
    }

    @Override
    public void onDestroyedByExplosion(World world, BlockPos pos, Explosion explosion) {
        super.onDestroyedByExplosion(world, pos, explosion);

        if (!(world instanceof ServerWorld serverWorld)) return;

        TwoTowersUtil.grantNearbyAdvancement(serverWorld, pos, 100);

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof ObeliskCoreBlockEntity core) {
            core.setHasPlaneMob(false); // reset in case a plane was active
            core.markDirty();
        }
    }
}

