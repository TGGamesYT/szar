package dev.tggamesyt.szar;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.Random;

public class SlotMachineScreenHandler extends ScreenHandler {

    public final SlotMachineBlockEntity blockEntity;
    private final SimpleInventory betInventory = new SimpleInventory(1);
    private final Random random = new Random();
    private final PlayerInventory playerInventory;
    private int currentBetAmount = 0;
    private ItemStack currentBetStack = ItemStack.EMPTY;

    private boolean spinning = false;
    private int spinTicks = 0;

    private SlotSymbol final0, final1, final2;

    public SlotMachineScreenHandler(int syncId, PlayerInventory playerInv, SlotMachineBlockEntity blockEntity) {
        super(Szar.SLOT_MACHINE_SCREEN_HANDLER_TYPE, syncId);
        this.playerInventory = playerInv;
        this.blockEntity = blockEntity;

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

        for (int y = 0; y < 3; y++)
            for (int x = 0; x < 9; x++)
                this.addSlot(new Slot(playerInv, x + y * 9 + 9, 8 + x * 18, 84 + y * 18));

        for (int x = 0; x < 9; x++)
            this.addSlot(new Slot(playerInv, x, 8 + x * 18, 142));
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {

        if (id != 0) return false;
        if (spinning) return false;

        ItemStack bet = betInventory.getStack(0);
        if (bet.isEmpty()) return false;

        // TAKE BET IMMEDIATELY
        currentBetAmount = bet.getCount();
        currentBetStack = bet.copy();
        betInventory.setStack(0, ItemStack.EMPTY);

        spinning = true;
        spinTicks = 60;

        final0 = SlotSymbol.roll(random);
        final1 = rollWithBias(final0);
        final2 = rollWithBias(final0, final1);

        return true;
    }

    public void tick(PlayerEntity player) {

        if (!spinning) return;

        spinTicks--;

        // Animate random symbols during spin
        if (spinTicks > 40) {
            blockEntity.setSymbols(
                    random.nextInt(7),
                    random.nextInt(7),
                    random.nextInt(7)
            );
        }

        // Lock first reel
        if (spinTicks == 40) {
            blockEntity.setSymbols(
                    final0.ordinal(),
                    random.nextInt(7),
                    random.nextInt(7)
            );
        }

        // Lock second reel
        if (spinTicks == 20) {
            blockEntity.setSymbols(
                    final0.ordinal(),
                    final1.ordinal(),
                    random.nextInt(7)
            );
        }

        // Lock third reel
        if (spinTicks == 0) {
            blockEntity.setSymbols(
                    final0.ordinal(),
                    final1.ordinal(),
                    final2.ordinal()
            );

            finishSpin(player);
            spinning = false;
        }
    }

    private SlotSymbol rollWithBias(SlotSymbol... biasToward) {

        float bonusChance = 0.20f; // 20% bonus chance toward existing symbol

        float r = random.nextFloat();

        if (r < bonusChance) {
            return biasToward[random.nextInt(biasToward.length)];
        }

        return SlotSymbol.roll(random);
    }

    private void finishSpin(PlayerEntity player) {

        int payout = 0;

        if (final0 == final1 && final1 == final2) {
            payout = switch (final0) {
                case SEVEN -> currentBetAmount * 100;
                case BELL -> currentBetAmount * 15;
                default -> currentBetAmount * 2;
            };
        }

        if (payout > 0) {
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
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasStack()) {
            ItemStack original = slot.getStack();
            newStack = original.copy();

            // Prevent shift-click while spinning
            if (spinning) {
                return ItemStack.EMPTY;
            }

            // If clicking bet slot → move to player inventory
            if (index == 0) {
                if (!this.insertItem(original, 1, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            }
            // If clicking player inventory → move to bet slot
            else {
                if (!this.insertItem(original, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (original.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return newStack;
    }

    @Override
    public void sendContentUpdates() {
        super.sendContentUpdates();

        if (!spinning) return;

        spinTicks--;

        int reel0;
        int reel1;
        int reel2;

        if (spinTicks > 40) {
            reel0 = random.nextInt(7);
        } else {
            reel0 = final0.ordinal();
        }

        if (spinTicks > 20) {
            reel1 = random.nextInt(7);
        } else {
            reel1 = final1.ordinal();
        }

// Reel 3 stops at tick 0
        if (spinTicks > 0) {
            reel2 = random.nextInt(7);
        } else {
            reel2 = final2.ordinal();
        }

        blockEntity.setSymbols(reel0, reel1, reel2);

        if (spinTicks <= 0) {
            finishSpin(playerInventory.player);
            spinning = false;
        }
    }
}