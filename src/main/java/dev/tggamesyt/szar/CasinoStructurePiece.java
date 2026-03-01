package dev.tggamesyt.szar;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.SimpleStructurePiece;
import net.minecraft.structure.StructureContext;
import net.minecraft.structure.StructurePlacementData;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.gen.structure.Structure;

public class CasinoStructurePiece extends SimpleStructurePiece {

    private static final Identifier TEMPLATE_ID =
            new Identifier(Szar.MOD_ID, "casino");

    /* ===== NORMAL CONSTRUCTOR (Worldgen) ===== */
    public CasinoStructurePiece(
            Structure.Context context,
            BlockPos pos,
            BlockPos origin,
            StructurePlacementData placement
    ) {
        super(
                Szar.CASINO_PIECE,
                0,
                context.structureTemplateManager(),
                TEMPLATE_ID,
                TEMPLATE_ID.toString(),
                placement,
                pos
        );
    }

    /* ===== NBT CONSTRUCTOR (Chunk Save/Load) ===== */
    public CasinoStructurePiece(StructureContext context, NbtCompound nbt) {
        super(
                Szar.CASINO_PIECE,
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
}