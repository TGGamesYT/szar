package dev.tggamesyt.szar.client.mixin;

import com.mojang.serialization.Lifecycle;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.world.GeneratorOptionsHolder;
import net.minecraft.world.gen.GeneratorOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(CreateWorldScreen.class)
public class CreateWorldScreenMixin {

    /**
     * Forces bypassWarnings=true on the tryLoad call inside createLevel(),
     * so the experimental/deprecated world warning is never shown.
     * The 5th parameter (index 4) of tryLoad is the boolean bypassWarnings.
     */
    @ModifyArg(
            method = "createLevel",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/integrated/IntegratedServerLoader;tryLoad(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/gui/screen/world/CreateWorldScreen;Lcom/mojang/serialization/Lifecycle;Ljava/lang/Runnable;Z)V"
            ),
            index = 4
    )
    private boolean forceBypassWarnings(boolean original) {
        return true;
    }
}
