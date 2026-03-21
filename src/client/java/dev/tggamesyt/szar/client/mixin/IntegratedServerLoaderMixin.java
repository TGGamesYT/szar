package dev.tggamesyt.szar.client.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.server.integrated.IntegratedServerLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IntegratedServerLoader.class)
public class IntegratedServerLoaderMixin {

    @Final
    @Shadow
    private MinecraftClient client;

    @Inject(
            method = "showBackupPromptScreen",
            at = @At("HEAD"),
            cancellable = true
    )
    private void skipBackupPrompt(
            Screen parent,
            String levelName,
            boolean customized,
            Runnable callback,
            CallbackInfo ci) {
        this.client.send(callback);
        ci.cancel();
    }
}