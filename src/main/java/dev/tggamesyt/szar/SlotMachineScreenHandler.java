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



    public final SlotMachineBlockEntity blockEntity;

    public SlotMachineScreenHandler(int syncId, PlayerInventory playerInv, SlotMachineBlockEntity blockEntity) {
        super(Szar.SLOT_MACHINE_SCREEN_HANDLER_TYPE, syncId);
        this.blockEntity = blockEntity;


        this.addProperties(blockEntity.propertyDelegate);

        // Bet slot
        this.addSlot(new Slot(blockEntity.betInventory, 0, 44, 35));

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
        return blockEntity.onButtonClicked(player, blockEntity);
    }
    /*
    @Override
    public void sendContentUpdates() {
        super.sendContentUpdates();

    }*/

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
        return blockEntity.propertyDelegate;
    }
}