package dev.tggamesyt.szar.client.mixin;

import dev.tggamesyt.szar.Szar;
import dev.tggamesyt.szar.client.RevolverScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class RevolverAttackMixin {

    @Inject(method = "onMouseButton", at = @At("HEAD"), cancellable = true)
    private void onMouseButton(long window, int button, int action, int mods, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (client.currentScreen != null) return; // let screens handle their own clicks
        if (button != 0 || action != 1) return; // only left click press

        ItemStack stack = client.player.getMainHandStack();
        if (!stack.isOf(Szar.REVOLVER)) return;

        ci.cancel(); // cancel vanilla handling entirely

        if (!client.player.isUsingItem()) {
            // Not aiming — open loading screen
            client.execute(() -> client.setScreen(new RevolverScreen(stack)));
        } else {
            // Aiming — shoot
            ClientPlayNetworking.send(Szar.REVOLVER_SHOOT, PacketByteBufs.create());
        }
    }
}