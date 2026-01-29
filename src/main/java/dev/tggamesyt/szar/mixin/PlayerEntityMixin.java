package dev.tggamesyt.szar.mixin;

import dev.tggamesyt.szar.Szar;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Inject(method = "initDataTracker", at = @At("TAIL"))
    private void szar$initTracker(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        player.getDataTracker().startTracking(Szar.LAST_CRIME_TICK, 0L);
    }
}
