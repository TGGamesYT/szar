package dev.tggamesyt.szar.client.mixin;

import dev.tggamesyt.szar.Szar;
import dev.tggamesyt.szar.client.RevolverHudState;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public class RevolverScrollMixin {

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.currentScreen != null) return;

        ItemStack stack = client.player.getMainHandStack();
        if (!stack.isOf(Szar.REVOLVER)) return;
        if (!RevolverHudState.isOpen) return;

        ci.cancel(); // don't scroll hotbar

        int direction = vertical > 0 ? 1 : -1;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(direction);
        ClientPlayNetworking.send(Szar.REVOLVER_SCROLL, buf);
    }
}