package dev.tggamesyt.szar;

import com.mojang.serialization.Codec;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.Optional;

public class IslandStructure extends Structure {

    public static final Codec<IslandStructure> CODEC =
            Structure.createCodec(IslandStructure::new);

    public IslandStructure(Config config) {
        super(config);
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        ChunkPos chunkPos = context.chunkPos();
        int x = chunkPos.getCenterX();
        int z = chunkPos.getCenterZ();

        // Find water surface — scan down from world height to sea level
        int seaLevel = context.chunkGenerator().getSeaLevel();

        int surfaceY = context.chunkGenerator().getHeightInGround(
                x, z,
                Heightmap.Type.OCEAN_FLOOR_WG,
                context.world(),
                context.noiseConfig()
        );

        // Must be underwater (ocean floor below sea level)
        if (surfaceY >= seaLevel - 2) return Optional.empty();

        // Place structure at sea level + 1 so it sits on the water surface
        BlockPos pos = new BlockPos(x, seaLevel + 1, z);

        StructurePlacementData placement = new StructurePlacementData()
                .setRotation(BlockRotation.random(context.random()));

        return Structure.getStructurePosition(context, Heightmap.Type.WORLD_SURFACE_WG, collector ->
                collector.addPiece(new IslandStructurePiece(context, pos, BlockPos.ORIGIN, placement))
        );
    }

    @Override
    public StructureType<?> getType() {
        return Szar.ISLAND_TYPE;
    }
}