package dev.tggamesyt.szar.mixin;

import dev.tggamesyt.szar.DrunkEffect;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(net.minecraft.entity.Entity.class)
public class EntityCollisionMixin {
    @Inject(method = "move", at = @At("TAIL"))
    private void szar_trackCollision(net.minecraft.entity.MovementType type,
                                     Vec3d movement, CallbackInfo ci) {
        net.minecraft.entity.Entity self = (net.minecraft.entity.Entity)(Object)this;
        if (!(self instanceof ServerPlayerEntity player)) return;
        if (self.horizontalCollision) {
            DrunkEffect.lastBlockCollisionTime.put(player.getUuid(),
                    player.getServerWorld().getTime());
        }
    }
}