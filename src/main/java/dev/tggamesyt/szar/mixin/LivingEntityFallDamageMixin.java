package dev.tggamesyt.szar.mixin;

import dev.tggamesyt.szar.PlaneEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityFallDamageMixin {

    // This injects at the start of computeFallDamage
    @Inject(method = "computeFallDamage", at = @At("HEAD"), cancellable = true)
    private void preventFallDamageIfOnPlane(float fallDistance, float damageMultiplier, CallbackInfoReturnable<Integer> cir) {
        LivingEntity self = (LivingEntity) (Object) this;

        // Check if the entity is a player riding a PlaneEntity
        if (self.hasVehicle() && self.getVehicle() instanceof PlaneEntity) {
            cir.setReturnValue(0); // Cancel fall damage
        }
    }
}