package dev.tggamesyt.szar;

import com.mojang.serialization.Codec;
import dev.tggamesyt.szar.PortalBlock;
import dev.tggamesyt.szar.Szar;
import dev.tggamesyt.szar.TrackerBlock;
import dev.tggamesyt.szar.TrackerBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;

public class OverworldPortalFeature extends Feature<DefaultFeatureConfig> {

    public OverworldPortalFeature(Codec<DefaultFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> ctx) {
        var world = ctx.getWorld();
        BlockPos origin = ctx.getOrigin();

        int surfaceY = world.getTopY(
                net.minecraft.world.Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                origin.getX(), origin.getZ());

        BlockPos trackerPos = new BlockPos(origin.getX(), surfaceY, origin.getZ());
        BlockPos portalPos = trackerPos.down(4);

        BlockState original = world.getBlockState(portalPos);

        world.setBlockState(trackerPos, Szar.TRACKER_BLOCK.getDefaultState(),
                net.minecraft.block.Block.NOTIFY_ALL);

        if (world.getBlockEntity(trackerPos) instanceof TrackerBlockEntity te) {
            te.placedByPlayer = false;
            te.originalPortalBlock = original;
            te.markDirty();
        }

        world.setBlockState(portalPos, Szar.PORTAL_BLOCK.getDefaultState(),
                net.minecraft.block.Block.NOTIFY_ALL);

        return true;
    }
}