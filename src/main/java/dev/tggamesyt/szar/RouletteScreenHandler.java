package dev.tggamesyt.szar;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

public class RouletteScreenHandler extends ScreenHandler {

    public final RouletteBlockEntity blockEntity;

    public static final int SLOT_SIZE = 18;
    public static final int GRID_START_X = 60;
    public static final int GRID_START_Y = 8;

    private static int gx(int col) { return GRID_START_X + (col - 1) * SLOT_SIZE; }
    private static int gy(int row) { return GRID_START_Y + (row - 1) * SLOT_SIZE; }

    // Slot that locks itself when the block entity is spinning
    private class BetSlot extends Slot {
        public BetSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        private boolean isSpinning() {
            return !blockEntity.isIntermission;
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return !isSpinning();
        }

        @Override
        public boolean canTakeItems(PlayerEntity playerEntity) {
            return !isSpinning();
        }

        @Override
        public ItemStack takeStack(int amount) {
            if (isSpinning()) return ItemStack.EMPTY;
            return super.takeStack(amount);
        }
    }

    public RouletteScreenHandler(int syncId, PlayerInventory playerInv, RouletteBlockEntity blockEntity) {
        super(Szar.ROULETTE_SCREEN_HANDLER_TYPE, syncId);
        this.blockEntity = blockEntity;

        this.addProperties(blockEntity.propertyDelegate);

        RouletteBlockEntity.PlayerBetInventories inv =
                blockEntity.getInventoriesFor(playerInv.player);

        // === fullbetInventory ===
        this.addSlot(new BetSlot(inv.fullbet, 0, gx(1), gy(2)));
        int fbIdx = 1;
        for (int col = 2; col <= 13; col++) {
            this.addSlot(new BetSlot(inv.fullbet, fbIdx++, gx(col), gy(3)));
            this.addSlot(new BetSlot(inv.fullbet, fbIdx++, gx(col), gy(2)));
            this.addSlot(new BetSlot(inv.fullbet, fbIdx++, gx(col), gy(1)));
        }

        // === twelvesInventory ===
        this.addSlot(new BetSlot(inv.twelves, 0, gx(3),  gy(4)));
        this.addSlot(new BetSlot(inv.twelves, 1, gx(7),  gy(4)));
        this.addSlot(new BetSlot(inv.twelves, 2, gx(11), gy(4)));

        // === halvesInventory ===
        this.addSlot(new BetSlot(inv.halves, 0, gx(2),  gy(5)));
        this.addSlot(new BetSlot(inv.halves, 1, gx(12), gy(5)));

        // === evenoddInventory ===
        this.addSlot(new BetSlot(inv.evenodd, 0, gx(4),  gy(5)));
        this.addSlot(new BetSlot(inv.evenodd, 1, gx(10), gy(5)));

        // === blackredInventory ===
        this.addSlot(new BetSlot(inv.blackred, 0, gx(6), gy(5)));
        this.addSlot(new BetSlot(inv.blackred, 1, gx(8), gy(5)));

        // === thirdsInventory ===
        this.addSlot(new BetSlot(inv.thirds, 0, gx(14), gy(1)));
        this.addSlot(new BetSlot(inv.thirds, 1, gx(14), gy(2)));
        this.addSlot(new BetSlot(inv.thirds, 2, gx(14), gy(3)));

        // === Player inventory ===
        int playerInvY = GRID_START_Y + 5 * SLOT_SIZE + 14;
        for (int y = 0; y < 3; y++)
            for (int x = 0; x < 9; x++)
                this.addSlot(new Slot(playerInv, x + y * 9 + 9, 8 + x * 18, playerInvY + y * 18));
        for (int x = 0; x < 9; x++)
            this.addSlot(new Slot(playerInv, x, 8 + x * 18, playerInvY + 58));
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        return true;
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

            int playerStart = 49;
            int totalSlots  = this.slots.size();

            if (index >= playerStart) {
                if (!this.insertItem(originalStack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index == 0) {
                if (!this.insertItem(originalStack, playerStart, totalSlots, true)) {
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
        return blockEntity.propertyDelegate;
    }
}