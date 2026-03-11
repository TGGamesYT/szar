package dev.tggamesyt.szar.client.mixin;

import dev.tggamesyt.szar.client.ResourcePackHelper;
import net.minecraft.client.gui.screen.pack.PackScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PackScreen.class)
public class PackScreenCloseMixin {

    @Inject(method = "close", at = @At("HEAD"))
    private void onClose(CallbackInfo ci) {
        ResourcePackHelper.applyAll(net.minecraft.client.MinecraftClient.getInstance());
    }
}