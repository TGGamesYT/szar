package dev.tggamesyt.szar.mixin;

import dev.tggamesyt.szar.DrunkEffect;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.entity.player.PlayerEntity.class)
public class PlayerSleepMixin {
    @Inject(method = "trySleep", at = @At("TAIL"))
    private void szar_trackSleep(net.minecraft.util.math.BlockPos pos,
                                 CallbackInfoReturnable<PlayerEntity.SleepFailureReason> cir) {
        net.minecraft.entity.player.PlayerEntity self =
                (net.minecraft.entity.player.PlayerEntity)(Object)this;
        if (self.getWorld().isClient) return;
        if (!(self.getWorld() instanceof net.minecraft.server.world.ServerWorld sw)) return;
        DrunkEffect.lastSleepTime.put(self.getUuid(), sw.getTime());
    }
}
