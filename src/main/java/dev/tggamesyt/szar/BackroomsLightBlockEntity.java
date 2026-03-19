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

    // Random offset so each light flickers at different times
    public int flickerOffset = 0;
    // How many ticks until next state toggle during flicker
    public int flickerTimer = 0;
    private boolean initialized = false;

    public BackroomsLightBlockEntity(BlockPos pos, BlockState state) {
        super(Szar.BACKROOMS_LIGHT_ENTITY, pos, state);
    }

    public static void tick(World world, BlockPos pos, BlockState state,
                            BackroomsLightBlockEntity entity) {
        if (!entity.initialized) {
            entity.flickerOffset = world.random.nextInt(100);
            entity.initialized = true;
            entity.markDirty();
        }

        BackroomsLightManager.tickLight(world, pos, state, entity);
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        nbt.putInt("FlickerOffset", flickerOffset);
        nbt.putInt("FlickerTimer", flickerTimer);
        nbt.putBoolean("Initialized", initialized);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        flickerOffset = nbt.getInt("FlickerOffset");
        flickerTimer = nbt.getInt("FlickerTimer");
        initialized = nbt.getBoolean("Initialized");
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }
}