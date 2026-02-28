package dev.tggamesyt.szar;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Random;

public class SlotMachineBlockEntity extends BlockEntity {

    public final int[] currentSymbol = new int[3];
    public static final int TOTAL_SYMBOLS = 7;

    public SlotMachineBlockEntity(BlockPos pos, BlockState state) {
        super(Szar.SLOT_MACHINE_BLOCKENTITY, pos, state);
    }

    public void setSymbols(int s0, int s1, int s2) {
        currentSymbol[0] = s0;
        currentSymbol[1] = s1;
        currentSymbol[2] = s2;
        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
    }

    public int getSymbol(int i) {
        return currentSymbol[i];
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        for (int i = 0; i < 3; i++) {
            nbt.putInt("Symbol" + i, currentSymbol[i]);
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        for (int i = 0; i < 3; i++) {
            currentSymbol[i] = nbt.getInt("Symbol" + i);
        }
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        NbtCompound nbt = new NbtCompound();
        writeNbt(nbt);
        return nbt;
    }
}