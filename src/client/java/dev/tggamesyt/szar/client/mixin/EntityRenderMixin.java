package dev.tggamesyt.szar.client.mixin;

import dev.tggamesyt.szar.client.SzarClient;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.time.LocalDate;

@Mixin(LivingEntityRenderer.class)
public class EntityRenderMixin<T extends LivingEntity> {

    @Unique
    private static boolean isAprilFools() {
        LocalDate today = LocalDate.now();
        return today.getMonthValue() == SzarClient.april && today.getDayOfMonth() == SzarClient.fools;
    }

    @Inject(
            method = "setupTransforms",
            at = @At("TAIL")
    )
    private void applyAprilFoolsFlip(T entity, MatrixStack matrices, float animationProgress, float bodyYaw, float tickDelta, CallbackInfo ci) {
        if (!isAprilFools()) return;

        if (LivingEntityRenderer.shouldFlipUpsideDown(entity)) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
            matrices.translate(0.0F, -(entity.getHeight() + 0.1F), 0.0F);
        } else {
            matrices.translate(0.0F, entity.getHeight() + 0.1F, 0.0F);
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180.0F));
        }
    }
}