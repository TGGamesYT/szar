package dev.tggamesyt.szar.client.mixin;

import dev.tggamesyt.szar.Szar;
import dev.tggamesyt.szar.client.MouseScrambleState;
import dev.tggamesyt.szar.client.SzarClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import static dev.tggamesyt.szar.client.SzarClient.MouseScrambleMode.*;

@Mixin(Mouse.class)
public abstract class MouseMixin {

    @Shadow private MinecraftClient client;

    @ModifyArgs(
            method = "updateMouse",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/network/ClientPlayerEntity;changeLookDirection(DD)V"
            )
    )
    private void szar$scrambleMouse(Args args) {

        if (client.player == null) return;

        if (!client.player.hasStatusEffect(Szar.DROG_EFFECT)) {
            MouseScrambleState.reset();
            return;
        }

        double dx = args.get(0);
        double dy = args.get(1);

        boolean isMoving = dx != 0 || dy != 0;

        var effect = client.player.getStatusEffect(Szar.DROG_EFFECT);
        assert effect != null;
        int amplifier = effect.getAmplifier();
        float level = amplifier + 1f;
        float chance = 0;
        if (level > 8) {chance = 0.25f * (level-8);}
        Random random = client.player.getRandom();

        /* ───── Mouse movement started ───── */
        if (isMoving && !MouseScrambleState.wasMoving) {

            MouseScrambleState.active = SzarClient.MouseScrambleMode.NONE;

            if (random.nextFloat() < chance) {
                SzarClient.MouseScrambleMode[] modes = {
                        INVERT_X,
                        INVERT_Y,
                        INVERT_BOTH
                };

                MouseScrambleState.active =
                        modes[random.nextInt(modes.length)];
            }
        }

        /* ───── Apply scramble ───── */
        switch (MouseScrambleState.active) {
            case INVERT_X -> dx = -dx;
            case INVERT_Y -> dy = -dy;
            case INVERT_BOTH -> {
                dx = -dx;
                dy = -dy;
            }
        }

        MouseScrambleState.wasMoving = isMoving;

        args.set(0, dx);
        args.set(1, dy);
    }
}
