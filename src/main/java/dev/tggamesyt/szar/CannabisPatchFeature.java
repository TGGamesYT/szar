package dev.tggamesyt.szar;

import com.mojang.serialization.Codec;
import dev.tggamesyt.szar.Szar;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

public class CannabisPatchFeature extends Feature<CannabisPatchFeatureConfig> {

    public CannabisPatchFeature(Codec<CannabisPatchFeatureConfig> codec) {
        super(codec);
    }

    @Override
    public boolean generate(FeatureContext<CannabisPatchFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos origin = context.getOrigin();
        Random random = context.getRandom();

        int cannabisCount = 2 + random.nextInt(5); // 2–6
        int tallCount = random.nextInt(3); // 0–2

        int placed = 0;

        for (int i = 0; i < cannabisCount + tallCount; i++) {
            BlockPos pos = origin.add(
                    random.nextInt(6) - 3,
                    0,
                    random.nextInt(6) - 3
            );

            pos = world.getTopPosition(
                    net.minecraft.world.Heightmap.Type.WORLD_SURFACE,
                    pos
            );

            if (!world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
                continue;
            }

            BlockState state =
                    tallCount > 0
                            ? Szar.TALL_CANNABIS_BLOCK.getDefaultState()
                            : Szar.CANNABIS_BLOCK.getDefaultState();

            if (tallCount > 0) tallCount--;

            world.setBlockState(pos, state, 3);
            placed++;
        }

        return placed > 0;
    }
}
