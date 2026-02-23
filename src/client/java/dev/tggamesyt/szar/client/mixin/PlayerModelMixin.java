package dev.tggamesyt.szar.client.mixin;

import dev.tggamesyt.szar.Szar;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityModel.class)
public abstract class PlayerModelMixin<T extends LivingEntity> {

    @Inject(method = "setAngles", at = @At("TAIL"))
    private void growTorso(T entity, float limbAngle, float limbDistance,
                           float animationProgress, float headYaw,
                           float headPitch, CallbackInfo ci) {

        if (!(entity instanceof PlayerEntity player)) return;

        PlayerEntityModel<?> model =
                (PlayerEntityModel<?>)(Object)this;

        // 🔁 RESET TO DEFAULT EVERY FRAME
        model.body.xScale = 1.0f;
        model.body.yScale = 1.0f;
        model.body.zScale = 1.0f;
        model.body.pivotZ = 0.0f;

        if (player.hasStatusEffect(Szar.PREGNANT)) {

            StatusEffectInstance effect =
                    player.getStatusEffect(Szar.PREGNANT);

            int maxDuration = 24000;
            float progress = 1f - (effect.getDuration() / (float) maxDuration);

            // slow, controlled growth
            float growth = progress * 0.5f; // max 0.5x longer

            model.body.zScale = 1.0f + growth;
            model.body.pivotZ = -growth * 4.0f;
        }
    }
}