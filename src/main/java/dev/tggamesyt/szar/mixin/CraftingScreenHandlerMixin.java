package dev.tggamesyt.szar.mixin;

import dev.tggamesyt.szar.RadiatedItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.CraftingInventory;
import net.minecraft.inventory.CraftingResultInventory;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.screen.CraftingScreenHandler;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CraftingScreenHandler.class)
public class CraftingScreenHandlerMixin {

    @Inject(
            method = "updateResult",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void onUpdateResult(ScreenHandler handler,
                                       World world,
                                       PlayerEntity player,
                                       RecipeInputInventory craftingInventory,
                                       CraftingResultInventory resultInventory,
                                       CallbackInfo ci) {

        boolean hasRadiated = false;
        ItemStack foodStack = ItemStack.EMPTY;

        for (int i = 0; i < craftingInventory.size(); i++) {
            ItemStack stack = craftingInventory.getStack(i);
            if (stack.isEmpty()) continue;

            if (stack.getItem() instanceof RadiatedItem) hasRadiated = true;
            else if (stack.isFood()) foodStack = stack;
        }

        if (hasRadiated && !foodStack.isEmpty()) {
            ItemStack resultStack = new ItemStack(foodStack.getItem());
            resultStack.setCount(1);
            resultStack.setNbt(foodStack.getNbt() != null ? foodStack.getNbt().copy() : null);
            resultStack.getOrCreateNbt().putBoolean("Radiated", true);

            resultInventory.setStack(0, resultStack);
            handler.setPreviousTrackedSlot(0, resultStack);

            if (player instanceof ServerPlayerEntity serverPlayer) {
                serverPlayer.networkHandler.sendPacket(
                        new ScreenHandlerSlotUpdateS2CPacket(handler.syncId, handler.nextRevision(), 0, resultStack)
                );
            }

            ci.cancel(); // prevent vanilla overwrite
        }
    }
}
