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
    public final int[] finalSymbol = new int[3];
    public static final int TOTAL_SYMBOLS = 7;
    private boolean spinning = false;
    private int spinTimer = 0;
    private int currentBetAmount = 0;
    private ItemStack currentBetStack = ItemStack.EMPTY;
    private boolean forceWin = false;
    private int winTier = 0; // 0 = fruit, 1 = golden apple small, 2 = golden apple jackpot

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

    public int getFinalSymbol(int i) {
        return finalSymbol[i];
    }

    public void setFinalSymbols(int s0, int s1, int s2) {
        finalSymbol[0] = s0;
        finalSymbol[1] = s1;
        finalSymbol[2] = s2;
        markDirty();
        if (world != null && !world.isClient) {
            world.updateListeners(pos, getCachedState(), getCachedState(), 3);
        }
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

    public void setSpinning(boolean spinning) {
        this.spinning = spinning;
    }

    public boolean getSpinning() {
        return spinning;
    }
    public void setspinTimer(int spinTimer) {
        this.spinTimer = spinTimer;
    }
    public int getspinTimer() {return spinTimer;}
    public void setcurrentBetAmount(int currentBetAmount) {this.currentBetAmount = currentBetAmount;}
    public int getcurrentBetAmount() {return currentBetAmount;}
    public void setcurrentBetStack(ItemStack currentBetStack) {this.currentBetStack = currentBetStack;}
    public ItemStack getcurrentBetStack() {return currentBetStack;}
    public void setForceWin(boolean forceWin) {this.forceWin = forceWin;}
    public boolean getForceWin() {return forceWin;}
    public void setwinTier(int winTier) {this.winTier = winTier;}
    public int getwinTier() {return winTier;}

    void finishSpin() {
        if (getForceWin()) {
            int payout = switch (getwinTier()) {
                case 0 -> getcurrentBetAmount() * 2;    // fruit 2x
                case 1 -> getcurrentBetAmount() * 10;   // golden apple small
                case 2 -> getcurrentBetAmount() * 30;  // jackpot
                default -> 0;
            };

            Direction facing = getCachedState().get(SlotMachineBlock.FACING);
            BlockPos drop = getPos().offset(facing);
            assert getWorld() != null;
            ItemScatterer.spawn(
                    getWorld(),
                    drop.getX(),
                    drop.getY(),
                    drop.getZ(),
                    new ItemStack(getcurrentBetStack().getItem(), payout)
            );
        }
        setcurrentBetAmount(0);
        setcurrentBetStack(ItemStack.EMPTY);
    }
}