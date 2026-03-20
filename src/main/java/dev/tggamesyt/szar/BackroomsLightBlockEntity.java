package dev.tggamesyt.szar;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BackroomsLightBlockEntity extends BlockEntity {
    public boolean isFlickering = false;
    // Used by renderer — offset into the global flicker timer
    public int flickerOffset = 0;
    // Visual brightness — only changed during blackout, not during flicker
    public float brightness = 1.0f;
    private boolean initialized = false;

    public BackroomsLightBlockEntity(BlockPos pos, BlockState state) {
        super(Szar.BACKROOMS_LIGHT_ENTITY, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state,
                            BackroomsLightBlockEntity entity) {
        if (!entity.initialized) {
            entity.flickerOffset = world.random.nextInt(1000);
            entity.initialized = true;
            entity.markDirty();
        }
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("FlickerOffset", flickerOffset);
        nbt.putFloat("Brightness", brightness);
        nbt.putBoolean("Initialized", initialized);
        nbt.putBoolean("IsFlickering", isFlickering);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        flickerOffset = nbt.getInt("FlickerOffset");
        brightness = nbt.getFloat("Brightness");
        initialized = nbt.getBoolean("Initialized");
        isFlickering = nbt.getBoolean("IsFlickering");
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() { return createNbt(); }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}