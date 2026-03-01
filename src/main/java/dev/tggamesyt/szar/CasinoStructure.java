package dev.tggamesyt.szar;

import com.mojang.serialization.Codec;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.Optional;

public class CasinoStructure extends Structure {

    public static final Codec<CasinoStructure> CODEC =
            Structure.createCodec(CasinoStructure::new);

    public CasinoStructure(Config config) {
        super(config);
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        return Structure.getStructurePosition(
                context,
                Heightmap.Type.WORLD_SURFACE_WG,
                collector -> {

                    ChunkPos chunkPos = context.chunkPos();
                    int x = chunkPos.getCenterX();
                    int z = chunkPos.getCenterZ();

                    int y = context.chunkGenerator().getHeightInGround(
                            x, z,
                            Heightmap.Type.WORLD_SURFACE_WG,
                            context.world(),
                            context.noiseConfig()
                    );

                    BlockPos pos = new BlockPos(x, y, z);

                    StructurePlacementData placement =
                            new StructurePlacementData()
                                    .setRotation(
                                            BlockRotation.random(context.random())
                                    );

                    collector.addPiece(
                            new CasinoStructurePiece(
                                    context,
                                    pos,
                                    BlockPos.ORIGIN,
                                    placement
                            )
                    );
                }
        );
    }

    @Override
    public StructureType<?> getType() {
        return Szar.CASINO_TYPE;
    }
}