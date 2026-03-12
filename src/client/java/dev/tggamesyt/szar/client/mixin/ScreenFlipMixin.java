package dev.tggamesyt.szar.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.tggamesyt.szar.client.SzarClient;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.LocalDate;

@Mixin(GameRenderer.class)
public class ScreenFlipMixin {

    @Unique
    private static boolean isAprilFools() {
        LocalDate today = LocalDate.now();
        return today.getMonthValue() == SzarClient.april && today.getDayOfMonth() == SzarClient.fools;
    }

    @ModifyReturnValue(method = "getBasicProjectionMatrix", at = @At("RETURN"))
    private Matrix4f flipProjection(Matrix4f original) {
        if (!isAprilFools()) return original;
        return original.scale(1.0f, -1.0f, 1.0f);
    }

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void setFrontFaceCW(float tickDelta, long limitTime, MatrixStack matrices, CallbackInfo ci) {
        if (!isAprilFools()) return;
        // Y flip reverses winding order, so tell GL that clockwise = front face
        GL11.glFrontFace(GL11.GL_CW);
    }

    @Inject(method = "renderWorld", at = @At("TAIL"))
    private void restoreFrontFaceCCW(float tickDelta, long limitTime, MatrixStack matrices, CallbackInfo ci) {
        if (!isAprilFools()) return;
        // Restore default: counter-clockwise = front face
        GL11.glFrontFace(GL11.GL_CCW);
    }
}