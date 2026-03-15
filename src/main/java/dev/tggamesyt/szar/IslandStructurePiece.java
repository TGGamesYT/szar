package dev.tggamesyt.szar;

import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.SimpleStructurePiece;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.structure.Structure;

public class IslandStructurePiece extends SimpleStructurePiece {

    private static final Identifier TEMPLATE_ID =
            new Identifier(Szar.MOD_ID, "island");

    /* ===== NORMAL CONSTRUCTOR (Worldgen) ===== */
    public IslandStructurePiece(
            Structure.Context context,
            BlockPos pos,
            BlockPos origin,
            StructurePlacementData placement
    ) {
        super(
                Szar.ISLAND_PIECE,
                0,
                context.structureTemplateManager(),
                TEMPLATE_ID,
                TEMPLATE_ID.toString(),
                placement,
                pos
        );
    }

    /* ===== NBT CONSTRUCTOR (Chunk Save/Load) ===== */
    public IslandStructurePiece(StructureContext context, NbtCompound nbt) {
        super(
                Szar.ISLAND_PIECE,
                nbt,
                context.structureTemplateManager(),
                identifier -> new StructurePlacementData()
        );
    }

    /* ===== Metadata Handler (DATA structure blocks) ===== */
    @Override
    protected void handleMetadata(
            String metadata,
            BlockPos pos,
            ServerWorldAccess world,
            Random random,
            BlockBox boundingBox
    ) {

    }

    @Override
    public void generate(StructureWorldAccess world, StructureAccessor structureAccessor,
                         ChunkGenerator chunkGenerator, Random random,
                         BlockBox chunkBox, ChunkPos chunkPos, BlockPos pivot) {

        // This actually places the structure blocks
        super.generate(world, structureAccessor, chunkGenerator, random, chunkBox, chunkPos, pivot);

        BlockBox box = this.getBoundingBox();
        for (int bx = box.getMinX(); bx <= box.getMaxX(); bx++) {
            for (int by = box.getMinY(); by <= box.getMaxY(); by++) {
                for (int bz = box.getMinZ(); bz <= box.getMaxZ(); bz++) {
                    BlockPos pos = new BlockPos(bx, by, bz);
                    if (world.getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                        chest.setLootTable(
                                new Identifier(Szar.MOD_ID, "chests/island"),
                                random.nextLong()
                        );
                    }
                }
            }
        }
    }
}