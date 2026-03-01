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

    private int spinTimer = 0;
    private boolean spinning = false;
    private boolean lastSpinWon = false;

    private int currentBetAmount = 0;
    private ItemStack currentBetStack = ItemStack.EMPTY;

    private SlotSymbol final0, final1, final2;

    private boolean forceWin = false;
    private int winTier = 0; // 0 = fruit, 1 = golden apple small, 2 = golden apple jackpot

    public SlotMachineScreenHandler(int syncId, PlayerInventory playerInv, SlotMachineBlockEntity blockEntity) {
        super(Szar.SLOT_MACHINE_SCREEN_HANDLER_TYPE, syncId);
        this.playerInventory = playerInv;
        this.blockEntity = blockEntity;

        this.propertyDelegate = new ArrayPropertyDelegate(2);
        this.addProperties(propertyDelegate);

        // Bet slot
        this.addSlot(new Slot(betInventory, 0, 44, 35) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return !spinning;
            }

            @Override
            public boolean canTakeItems(PlayerEntity playerEntity) {
                return !spinning;
            }
        });

        // Player inventory slots
        for (int y = 0; y < 3; y++)
            for (int x = 0; x < 9; x++)
                this.addSlot(new Slot(playerInv, x + y * 9 + 9, 8 + x * 18, 84 + y * 18));
        for (int x = 0; x < 9; x++)
            this.addSlot(new Slot(playerInv, x, 8 + x * 18, 142));
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id != 0 || spinning) return false;

        ItemStack bet = betInventory.getStack(0);
        if (bet.isEmpty()) return false;

        currentBetAmount = bet.getCount();
        currentBetStack = bet.copy();
        betInventory.setStack(0, ItemStack.EMPTY);

        // === Determine if this spin will definitely win (40%) ===
        forceWin = random.nextFloat() < 0.4f;
        if (forceWin) {
            float tierRoll = random.nextFloat();
            if (tierRoll < 0.80f) winTier = 0;        // Fruit win (2x items)
            else if (tierRoll < 0.96f) winTier = 1;   // Golden Apple small (25x)
            else winTier = 2;                          // Jackpot (100x)
        } else {
            winTier = -1; // no win
        }

        // === Preselect final symbols based on forced win type ===
        if (forceWin) {
            switch (winTier) {
                case 0 -> { // fruit
                    final0 = SlotSymbol.rollFruit(random);
                    final1 = final0;
                    final2 = final0;
                }
                case 1 -> { // golden apple small
                    final0 = SlotSymbol.BELL;
                    final1 = final0;
                    final2 = final0;
                }
                case 2 -> { // jackpot
                    final0 = SlotSymbol.SEVEN;
                    final1 = final0;
                    final2 = final0;
                }
            }
        } else {
            final0 = SlotSymbol.roll(random);
            final1 = SlotSymbol.roll(random);
            final2 = SlotSymbol.roll(random);
            if (final0 == final1 && final1 == final2) {
                forceWin = true;
                winTier = final0 == SlotSymbol.BELL ? 1 : final0 == SlotSymbol.SEVEN ? 2 : 0;
            }
        }

        spinTimer = 0;
        spinning = true;
        propertyDelegate.set(0, 1);

        return true;
    }

    @Override
    public void sendContentUpdates() {
        super.sendContentUpdates();

        if (!spinning) {
            if (blockEntity.getWorld().getTime() % IDLE_SPEED == 0) {
                blockEntity.setSymbols(
                        random.nextInt(7),
                        random.nextInt(7),
                        random.nextInt(7)
                );
            }
            return;
        }

        spinTimer++;

        int totalSpinDuration =
                PREPARE_TIME +
                        FAST_SPIN_TIME +
                        LOCK_INTERVAL * 3 +
                        RESULT_VIEW_TIME;

        int speed = switch (spinTimer < PREPARE_TIME ? 0 : spinTimer < PREPARE_TIME + FAST_SPIN_TIME ? 1 : 2) {
            case 0 -> PREPARE_SPEED;
            case 1 -> FAST_SPEED;
            default -> FAST_SPEED;
        };

        boolean lock0 = spinTimer >= PREPARE_TIME + FAST_SPIN_TIME + LOCK_INTERVAL;
        boolean lock1 = spinTimer >= PREPARE_TIME + FAST_SPIN_TIME + LOCK_INTERVAL * 2;
        boolean lock2 = spinTimer >= PREPARE_TIME + FAST_SPIN_TIME + LOCK_INTERVAL * 3;

        int reel0 = lock0 ? final0.ordinal() : random.nextInt(7);
        int reel1 = lock1 ? final1.ordinal() : random.nextInt(7);
        int reel2 = lock2 ? final2.ordinal() : random.nextInt(7);

        if (spinTimer % speed == 0) {
            blockEntity.setSymbols(reel0, reel1, reel2);
        }
        if (spinTimer >= (totalSpinDuration - RESULT_VIEW_TIME + 15)) {
            propertyDelegate.set(1, forceWin ? 1 : 0);
        }
        if (spinTimer >= totalSpinDuration) {
            finishSpin(playerInventory.player);
            spinning = false;
            propertyDelegate.set(0, 0);
        }
    }

    private void finishSpin(PlayerEntity player) {
        lastSpinWon = forceWin;

        if (lastSpinWon) {
            int payout = switch (winTier) {
                case 0 -> currentBetAmount * 2;    // fruit 2x
                case 1 -> currentBetAmount * 16;   // golden apple small
                case 2 -> currentBetAmount * 32;  // jackpot
                default -> 0;
            };

            Direction facing = blockEntity.getCachedState().get(SlotMachineBlock.FACING);
            BlockPos drop = blockEntity.getPos().offset(facing);

            ItemScatterer.spawn(
                    player.getWorld(),
                    drop.getX(),
                    drop.getY(),
                    drop.getZ(),
                    new ItemStack(currentBetStack.getItem(), payout)
            );
        }
        currentBetAmount = 0;
        currentBetStack = ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        if (spinning) return ItemStack.EMPTY;
        return ItemStack.EMPTY;
    }

    public PropertyDelegate getPropertyDelegate() {
        return propertyDelegate;
    }
    public int getSpinTimer() {
        return spinTimer;
    }

    public boolean didLastSpinWin() {
        return forceWin;
    }
}