package dev.tggamesyt.szar.client.mixin;

import dev.tggamesyt.szar.Szar;
import dev.tggamesyt.szar.client.CustomLogoTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.IntSupplier;

@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {

    @Shadow @Final @Mutable
    private static Identifier LOGO;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void modifyStatic(CallbackInfo ci) {
        LOGO = new Identifier(Szar.MOD_ID, "textures/gui/szarmod.png");
    }

    @Inject(method = "init", at = @At("HEAD"), cancellable = true)
    private static void replaceInit(MinecraftClient client, CallbackInfo ci) {
        client.getTextureManager().registerTexture(LOGO, new CustomLogoTexture(LOGO));
        ci.cancel();
    }
    @Unique
    private static final float SCALE = 2.0F;

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIFFIIII)V"
            )
    )
    private void scaleLogo(
            DrawContext context,
            Identifier texture,
            int x, int y,
            int width, int height,
            float u, float v,
            int regionWidth, int regionHeight,
            int textureWidth, int textureHeight
    ) {

        if (!texture.equals(LOGO)) {
            context.drawTexture(texture, x, y,
                    width, height,
                    u, v,
                    regionWidth, regionHeight,
                    textureWidth, textureHeight);
            return;
        }

        int scaledWidth = (int)(width * SCALE);
        int scaledHeight = (int)(height * SCALE);

        int centerX;

        // Determine shared center
        if (u < 0) {
            // Left half
            centerX = x + width;
            x = centerX - scaledWidth;
        } else {
            // Right half
            centerX = x;
        }

        // Vertically scale from center
        int centerY = y + height / 2;
        y = centerY - scaledHeight / 2;

        context.drawTexture(texture,
                x, y,
                scaledWidth, scaledHeight,
                u, v,
                regionWidth, regionHeight,
                textureWidth, textureHeight);
    }
}
