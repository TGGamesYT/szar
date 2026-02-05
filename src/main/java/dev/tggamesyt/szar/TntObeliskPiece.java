package dev.tggamesyt.szar;

import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePiece;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

public class TntObeliskPiece extends StructurePiece {

    private final BlockPos base;
    private final int height;
    private final int size; // width of the tower (square)

    /* ===== NORMAL CONSTRUCTOR ===== */
    public TntObeliskPiece(BlockPos base, int height, int size) {
        super(
                Szar.TNT_OBELISK_PIECE,
                0,
                new BlockBox(
                        base.getX(),
                        base.getY(),
                        base.getZ(),
                        base.getX() + size - 1,
                        base.getY() + height - 1,
                        base.getZ() + size - 1
                )
        );
        this.base = base;
        this.height = height;
        this.size = size;
    }

    /* ===== NBT CONSTRUCTOR ===== */
    public TntObeliskPiece(StructureContext context, NbtCompound nbt) {
        super(Szar.TNT_OBELISK_PIECE, nbt);
        this.base = BlockPos.fromLong(nbt.getLong("Base"));
        this.height = nbt.getInt("Height");
        this.size = nbt.getInt("Size");
    }

    /* ===== SAVE DATA ===== */
    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        nbt.putLong("Base", base.asLong());
        nbt.putInt("Height", height);
        nbt.putInt("Size", size);
    }

    /* ===== PLACE BLOCKS ===== */
    @Override
    public void generate(
            StructureWorldAccess world,
            StructureAccessor accessor,
            ChunkGenerator chunkGenerator,
            Random random,
            BlockBox box,
            ChunkPos chunkPos,
            BlockPos pivot
    ) {
        // Core position (centered)
        int coreY = height / 2;
        int coreX = size / 2;
        int coreZ = size / 2;

        for (int y = 0; y < height; y++) {
            for (int dx = 0; dx < size; dx++) {
                for (int dz = 0; dz < size; dz++) {

                    BlockPos pos = base.add(dx, y, dz);
                    if (!box.contains(pos)) continue;

                    boolean isEdge =
                            dx == 0 || dz == 0 ||
                                    dx == size - 1 || dz == size - 1;

                    boolean isTopOrBottom =
                            y == 0 || y == height - 1;

                    // === PLACE CORE BLOCK (EXACTLY ONCE) ===
                    if (y == coreY && dx == coreX && dz == coreZ) {
                        world.setBlockState(
                                pos,
                                Szar.OBELISK_CORE.getDefaultState(),
                                2
                        );
                        continue;
                    }

                    // === SHELL + CAPS ===
                    if (isEdge || isTopOrBottom) {
                        world.setBlockState(
                                pos,
                                Blocks.ANDESITE.getDefaultState(),
                                2
                        );
                    }
                    // === INTERIOR ===
                    else {
                        world.setBlockState(
                                pos,
                                Blocks.TNT.getDefaultState(),
                                2
                        );
                    }
                }
            }
        }
    }

}
