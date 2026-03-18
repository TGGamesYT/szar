package dev.tggamesyt.szar;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class WallBlockEntity extends BlockEntity {

    // -1 = no drawing, 0-7 = which drawing
    public int drawingIndex = -1;
    // Which face the drawing is on (0=north, 1=south, 2=west, 3=east)
    public int drawingFace = 0;
    public boolean initialized = false;

    public WallBlockEntity(BlockPos pos, BlockState state) {
        super(Szar.WALL_BLOCK_ENTITY, pos, state);
    }

    public void initializeIfNeeded() {
        if (initialized) return;
        initialized = true;

        java.util.Random rand = new java.util.Random(
                // Seed by position so it's deterministic per block
                pos.getX() * 341873128712L ^ pos.getZ() * 132897987541L ^ pos.getY()
        );

        if (rand.nextInt(25) == 0) {
            drawingIndex = rand.nextInt(8);
            drawingFace = rand.nextInt(4);
        }

        markDirty();
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("DrawingIndex", drawingIndex);
        nbt.putInt("DrawingFace", drawingFace);
        nbt.putBoolean("Initialized", initialized);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        drawingIndex = nbt.getInt("DrawingIndex");
        drawingFace = nbt.getInt("DrawingFace");
        initialized = nbt.getBoolean("Initialized");
    }

    public static void tick(World world, BlockPos pos, BlockState state, WallBlockEntity entity) {
        if (!world.isClient && !entity.initialized) {
            entity.initializeIfNeeded();
        }
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    // In WallBlockEntity.java — override this to auto-init when chunk data is requested
    @Override
    public NbtCompound toInitialChunkDataNbt() {
        if (!initialized) {
            initializeIfNeeded();
        }
        return createNbt();
    }
}