package dev.tggamesyt.szar.mixin;

import dev.tggamesyt.szar.PlaneEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerInteractionMixin {

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void preventPlaneInteraction(Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.getVehicle() instanceof PlaneEntity) {
            cir.setReturnValue(ActionResult.FAIL); // cancel interaction
        }
    }
}