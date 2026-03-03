package dev.tggamesyt.szar;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Random;

public class SlotMachineScreenHandler extends ScreenHandler {

    // ===== TIMING CONFIG =====
    public static final int PREPARE_TIME = 35;
    public static final int FAST_SPIN_TIME = 35;
    public static final int LOCK_INTERVAL = 8;
    private static final int RESULT_VIEW_TIME = 80;

    private static final int IDLE_SPEED = 20;
    private static final int PREPARE_SPEED = 8;
    private static final int FAST_SPEED = 1;

    public final SlotMachineBlockEntity blockEntity;
    private final SimpleInventory betInventory = new SimpleInventory(1);
    private final PlayerInventory playerInventory;
    private final PropertyDelegate propertyDelegate;
    private final Random random = new Random();

    public SlotMachineScreenHandler(int syncId, PlayerInventory playerInv, SlotMachineBlockEntity blockEntity) {
        super(Szar.SLOT_MACHINE_SCREEN_HANDLER_TYPE, syncId);
        this.playerInventory = playerInv;
        this.blockEntity = blockEntity;

        this.propertyDelegate = new ArrayPropertyDelegate(2);
        this.addProperties(propertyDelegate);

        // Bet slot
        this.addSlot(new Slot(betInventory, 0, 44, 35));

        // Player inventory slots
        for (int y = 0; y < 3; y++)
            for (int x = 0; x < 9; x++)
                this.addSlot(new Slot(playerInv, x + y * 9 + 9, 8 + x * 18, 84 + y * 18));
        for (int x = 0; x < 9; x++)
            this.addSlot(new Slot(playerInv, x, 8 + x * 18, 142));
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id != 0 || blockEntity.getSpinning()) return false;

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
        propertyDelegate.set(0, 1);

        return true;
    }

    @Override
    public void sendContentUpdates() {
        super.sendContentUpdates();

        if (!blockEntity.getSpinning()) {
            if (blockEntity.getWorld().getTime() % IDLE_SPEED == 0) {
                blockEntity.setSymbols(
                        random.nextInt(7),
                        random.nextInt(7),
                        random.nextInt(7)
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

        int reel0 = lock0 ? blockEntity.getFinalSymbol(0) : random.nextInt(7);
        int reel1 = lock1 ? blockEntity.getFinalSymbol(1) : random.nextInt(7);
        int reel2 = lock2 ? blockEntity.getFinalSymbol(2) : random.nextInt(7);

        if (blockEntity.getspinTimer() % speed == 0) {
            blockEntity.setSymbols(reel0, reel1, reel2);
        }
        if (blockEntity.getspinTimer() >= (totalSpinDuration - RESULT_VIEW_TIME + 15)) {
            propertyDelegate.set(1, blockEntity.getForceWin() ? 1 : 0);
        }
        if (blockEntity.getspinTimer() >= totalSpinDuration) {
            blockEntity.finishSpin();
            blockEntity.setSpinning(false);
            propertyDelegate.set(0, 0);
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {

        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            // If clicking the bet slot → move to player inventory
            if (index == 0) {
                if (!this.insertItem(originalStack, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            }
            // If clicking player inventory → move to bet slot
            else {
                if (!this.insertItem(originalStack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return newStack;
    }

    public PropertyDelegate getPropertyDelegate() {
        return propertyDelegate;
    }
}