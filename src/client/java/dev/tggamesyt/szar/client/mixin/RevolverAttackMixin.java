package dev.tggamesyt.szar.client.mixin;

import dev.tggamesyt.szar.Szar;
import dev.tggamesyt.szar.client.AK47InputState;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class RevolverAttackMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (client.currentScreen != null) return;
        if (button != 0) return;

        ItemStack stack = client.player.getMainHandStack();

        if (stack.isOf(Szar.REVOLVER)) {
            if (action != 1) return;
            ci.cancel();
            if (client.player.isUsingItem()) {
                ClientPlayNetworking.send(Szar.REVOLVER_SHOOT, PacketByteBufs.create());
            }
            return;
        }

        if (stack.isOf(Szar.AK47)) {
            ci.cancel();
            if (action == 1) AK47InputState.mouseHeld = true;
            if (action == 0) AK47InputState.mouseHeld = false;
        }
    }
}