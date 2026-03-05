package dev.tggamesyt.szar;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Random;

public class SlotMachineBlockEntity extends BlockEntity {

    // ===== TIMING CONFIG =====
    public static final int PREPARE_TIME = 35;
    public static final int FAST_SPIN_TIME = 35;
    public static final int LOCK_INTERVAL = 8;
    private static final int RESULT_VIEW_TIME = 80;

    private static final int IDLE_SPEED = 20;
    private static final int PREPARE_SPEED = 8;
    private static final int FAST_SPEED = 1;

    private final Random random = new Random();

    final SimpleInventory betInventory = new SimpleInventory(1);
    public final int[] currentSymbol = new int[3];
    public final int[] finalSymbol = new int[3];
    public static final int TOTAL_SYMBOLS = 7;
    private boolean spinning = false;
    private int spinTimer = 0;
    private int currentBetAmount = 0;
    private ItemStack currentBetStack = ItemStack.EMPTY;
    private boolean forceWin = false;
    private int winTier = 0; // 0 = fruit, 1 = golden apple small, 2 = golden apple jackpot
    private boolean isHandleClicked = false;
    private PlayerEntity clickedplayer;
    private int handleClickWaittime = 0;

    final PropertyDelegate propertyDelegate;

    public SlotMachineBlockEntity(BlockPos pos, BlockState state) {
        super(Szar.SLOT_MACHINE_BLOCKENTITY, pos, state);
        this.propertyDelegate = new ArrayPropertyDelegate(2);
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
    public boolean isHandleClicked(PlayerEntity player) {return clickedplayer != player ? isHandleClicked : false;}
    public void setHandleClicked(boolean isHandleClicked, PlayerEntity player) {this.isHandleClicked =  isHandleClicked; this.clickedplayer = player;}

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
    public static void tick(World world, BlockPos pos, BlockState state, SlotMachineBlockEntity blockEntity) {
        if (world.isClient) return;
        if (blockEntity.isHandleClicked) {
            blockEntity.handleClickWaittime++;
        }
        if (blockEntity.handleClickWaittime >= 1) {
            blockEntity.isHandleClicked = false;
        }

        // old sendcontentupdates
        if (!blockEntity.getSpinning()) {
            if (blockEntity.getWorld().getTime() % IDLE_SPEED == 0) {
                blockEntity.setSymbols(
                        blockEntity.random.nextInt(7),
                        blockEntity.random.nextInt(7),
                        blockEntity.random.nextInt(7)
                );
            }
            return;
        }

        blockEntity.setspinTimer(blockEntity.getspinTimer() + 1);

        int totalSpinDuration =
                PREPARE_TIME +
                        FAST_SPIN_TIME +
                        LOCK_INTERVAL * 3 +
                        RESULT_VIEW_TIME;

        int speed = switch (blockEntity.getspinTimer() < PREPARE_TIME ? 0 : blockEntity.getspinTimer() < PREPARE_TIME + FAST_SPIN_TIME ? 1 : 2) {
            case 0 -> PREPARE_SPEED;
            case 1 -> FAST_SPEED;
            default -> FAST_SPEED;
        };

        boolean lock0 = blockEntity.getspinTimer() >= PREPARE_TIME + FAST_SPIN_TIME + LOCK_INTERVAL;
        boolean lock1 = blockEntity.getspinTimer() >= PREPARE_TIME + FAST_SPIN_TIME + LOCK_INTERVAL * 2;
        boolean lock2 = blockEntity.getspinTimer() >= PREPARE_TIME + FAST_SPIN_TIME + LOCK_INTERVAL * 3;

        int reel0 = lock0 ? blockEntity.getFinalSymbol(0) : blockEntity.random.nextInt(7);
        int reel1 = lock1 ? blockEntity.getFinalSymbol(1) : blockEntity.random.nextInt(7);
        int reel2 = lock2 ? blockEntity.getFinalSymbol(2) : blockEntity.random.nextInt(7);

        if (blockEntity.getspinTimer() % speed == 0) {
            blockEntity.setSymbols(reel0, reel1, reel2);
        }
        if (blockEntity.getspinTimer() >= (totalSpinDuration - RESULT_VIEW_TIME + 15)) {
            blockEntity.propertyDelegate.set(1, blockEntity.getForceWin() ? 1 : 0);
        }
        if (blockEntity.getspinTimer() >= totalSpinDuration) {
            blockEntity.finishSpin();
            blockEntity.setSpinning(false);
            blockEntity.propertyDelegate.set(0, 0);
        }
    }

    public boolean onButtonClicked(PlayerEntity player, SlotMachineBlockEntity blockEntity) {
        blockEntity.setHandleClicked(true, player);
        ItemStack bet = betInventory.getStack(0);
        if (bet.isEmpty()) return false;

        blockEntity.setcurrentBetAmount(bet.getCount());
        blockEntity.setcurrentBetStack(bet.copy());
        betInventory.setStack(0, ItemStack.EMPTY);

        // === Determine if this spin will definitely win (40%) ===
        blockEntity.setForceWin(random.nextFloat() < 0.4f);
        if (blockEntity.getForceWin()) {
            float tierRoll = random.nextFloat();
            if (tierRoll < 0.88f) blockEntity.setwinTier(0);        // 88%
            else if (tierRoll < 0.98f) blockEntity.setwinTier(1);   // 10%
            else blockEntity.setwinTier(2);                          // 2%
        } else {
            blockEntity.setwinTier(-1);
        }

        // === Preselect final symbols based on forced win type ===
        if (blockEntity.getForceWin()) {
            switch (blockEntity.getwinTier()) {
                case 0 -> { // fruit
                    int symbol = SlotSymbol.symbolToInt(SlotSymbol.rollFruit(random));
                    blockEntity.setFinalSymbols(symbol, symbol, symbol);
                }
                case 1 -> { // golden apple small
                    int symbol = SlotSymbol.symbolToInt(SlotSymbol.BELL);
                    blockEntity.setFinalSymbols(symbol, symbol, symbol);
                }
                case 2 -> { // jackpot
                    int symbol = SlotSymbol.symbolToInt(SlotSymbol.SEVEN);
                    blockEntity.setFinalSymbols(symbol, symbol, symbol);
                }
            }
        } else {
            blockEntity.setFinalSymbols(SlotSymbol.symbolToInt(SlotSymbol.roll(random)), SlotSymbol.symbolToInt(SlotSymbol.roll(random)), SlotSymbol.symbolToInt(SlotSymbol.roll(random)));

            if (blockEntity.getFinalSymbol(0) == blockEntity.getFinalSymbol(1) && blockEntity.getFinalSymbol(1) == blockEntity.getFinalSymbol(2)) {
                blockEntity.setForceWin(true);
                blockEntity.setwinTier(SlotSymbol.intToSymbol(blockEntity.getFinalSymbol(0)) == SlotSymbol.BELL ? 1 : SlotSymbol.intToSymbol(blockEntity.getFinalSymbol(0)) == SlotSymbol.SEVEN ? 2 : 0);
            }
        }

        blockEntity.setspinTimer(0);
        blockEntity.setSpinning(true);
        blockEntity.propertyDelegate.set(0, 1);

        return true;
    }
}