package dev.tggamesyt.szar.client.mixin;

import dev.tggamesyt.szar.Szar;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(InGameHud.HeartType.class)
public class RadiationHeartMixin {

    @Inject(
            method = "fromPlayerState",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void radiationHeart(PlayerEntity player, CallbackInfoReturnable<InGameHud.HeartType> cir) {
        if (player.hasStatusEffect(Szar.RADIATION)) {
            cir.setReturnValue(InGameHud.HeartType.POISONED);
        }
    }
}
