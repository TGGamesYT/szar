package dev.tggamesyt.szar.client.mixin;

import dev.tggamesyt.szar.Joint;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {

    @Shadow @Final private MinecraftClient client;

    // smooth value between 0.0 (no zoom) and 1.0 (full zoom)
    @Unique private float szar$jointZoomProgress = 0.0F;

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void injectJointFov(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Double> cir) {
        if (this.client.player == null) return;

        boolean isUsing = this.client.player.isUsingItem()
                && this.client.player.getActiveItem().getItem() instanceof Joint;

        // ease in when using, ease out when not
        if (isUsing) {
            szar$jointZoomProgress = Math.min(1.0F, szar$jointZoomProgress + tickDelta * 0.1F);
        } else {
            szar$jointZoomProgress = Math.max(0.0F, szar$jointZoomProgress - tickDelta * 0.1F);
        }

        if (szar$jointZoomProgress <= 0.0F) return;

        // smooth S-curve easing
        float eased = szar$jointZoomProgress * szar$jointZoomProgress * (3.0F - 2.0F * szar$jointZoomProgress);

        double currentFov = cir.getReturnValue();
        // lerp toward 90% of normal FOV (10% zoom)
        cir.setReturnValue(currentFov * (1.0 - 0.1 * eased));
    }
}