package dev.tggamesyt.szar.mixin;

import dev.tggamesyt.szar.RadiatedItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.CraftingResultSlot;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingResultSlot.class)
public class CraftingScreenHandlerMixin2 {

    @Inject(
            method = "onTakeItem",
            at = @At("HEAD")
    )
    private void onTakeRadiatedItem(PlayerEntity player, ItemStack stack, CallbackInfo ci) {
        if (stack.hasNbt() && stack.getNbt().getBoolean("Radiated")) {
            ScreenHandler handler = player.currentScreenHandler;

            // Only modify the 2x2 or 3x3 crafting grid slots (1-9)
            for (int i = 1; i <= 9; i++) {
                if (i >= handler.slots.size()) break; // safety check for 2x2
                Slot slot = handler.slots.get(i);
                ItemStack slotStack = slot.getStack();
                if (!slotStack.isEmpty()) {
                    // Halve and round up
                    int newCount = (slotStack.getCount() / 2);
                    slotStack.setCount(newCount);
                    slot.markDirty();
                }
            }
        }
    }
}
