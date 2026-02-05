package dev.tggamesyt.szar;

import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.explosion.Explosion;

public class ObeliskCoreBlock extends Block {

    public ObeliskCoreBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onDestroyedByExplosion(World world, BlockPos pos, Explosion explosion) {
        super.onDestroyedByExplosion(world, pos, explosion);

        if (!(world instanceof ServerWorld serverWorld)) return;

        TwoTowersUtil.grantNearbyAdvancement(serverWorld, pos, 100);
    }
}
