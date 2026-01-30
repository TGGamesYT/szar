package dev.tggamesyt.szar;

import com.mojang.serialization.Codec;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.Optional;

public class TntObeliskStructure extends Structure {

    public static final Codec<TntObeliskStructure> CODEC =
            Structure.createCodec(TntObeliskStructure::new);

    public TntObeliskStructure(Config config) {
        super(config);
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        return Structure.getStructurePosition(
                context,
                Heightmap.Type.WORLD_SURFACE_WG,
                collector -> {

                    ChunkRandom random = context.random();
                    ChunkPos chunkPos = context.chunkPos();

                    int x = chunkPos.getCenterX();
                    int z = chunkPos.getCenterZ();

                    int y = context.chunkGenerator().getHeightInGround(
                            x, z,
                            Heightmap.Type.WORLD_SURFACE_WG,
                            context.world(),
                            context.noiseConfig()
                    );

                    BlockPos base1 = new BlockPos(x, y, z);

                    int size = 6 + random.nextInt(2); // 6–7 wide

                    int height1 = 50 + random.nextInt(51); // 50–100
                    int height2 = height1 - (8 + random.nextInt(5)); // ~10 diff

                    int gap = 4 + random.nextInt(3); // 4–6 gap

                    // Parallel: offset ONLY on X axis
                    BlockPos base2 = base1.add(size + gap, 0, 0);

                    collector.addPiece(
                            new TntObeliskPiece(base1, height1, size)
                    );
                    collector.addPiece(
                            new TntObeliskPiece(base2, height2, size)
                    );
                }
        );
    }

    @Override
    public StructureType<?> getType() {
        return Szar.TNT_OBELISK_TYPE;
    }
}
