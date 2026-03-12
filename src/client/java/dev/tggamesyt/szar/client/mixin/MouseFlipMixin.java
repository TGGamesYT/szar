package dev.tggamesyt.szar.client.mixin;

import dev.tggamesyt.szar.client.SzarClient;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.time.LocalDate;

@Mixin(Mouse.class)
public class MouseFlipMixin {

    @Unique
    private static boolean isAprilFools() {
        LocalDate today = LocalDate.now();
        return today.getMonthValue() == SzarClient.april && today.getDayOfMonth() == SzarClient.fools;
    }

    @ModifyArg(
            method = "updateMouse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"
            ),
            index = 1
    )
    private double flipMouseY(double pitchDelta) {
        if (!isAprilFools()) return pitchDelta;
        return -pitchDelta;
    }
}