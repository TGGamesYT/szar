package dev.tggamesyt.szar.mixin;

import dev.tggamesyt.szar.PlaneEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public class PlaneBlockInteractionMixin {

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void interactBlock(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (player.getVehicle() instanceof PlaneEntity) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }

    @Inject(method = "tryBreakBlock", at = @At("HEAD"), cancellable = true)
    private void preventBlockBreaking(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayerEntity player = ((ServerPlayerInteractionManager)(Object)this).player;
        if (player.getVehicle() instanceof PlaneEntity) {
            cir.setReturnValue(false);
        }
    }
}