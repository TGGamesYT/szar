package dev.tggamesyt.szar.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.tggamesyt.szar.client.SzarClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Mixin(TitleScreen.class)
public class TitleScreenBackgroundMixin {
    @Unique
    private static final Identifier VANILLA_OVERLAY =
            new Identifier("textures/gui/title/background/panorama_overlay.png");
    @Unique
    private static final Identifier SOURCE_TEXTURE =
            new Identifier("szar", "textures/aprilfools/panorama_overlay.png");
    @Unique
    private static final List<Identifier> FRAMES = new ArrayList<>();
    @Unique
    private static long lastFrameTime = 0;
    @Unique
    private static int currentFrame = 0;
    @Unique
    private static boolean framesLoaded = false;

    // frametime 1 = 1 game tick = 50ms
    @Unique
    private static final long FRAME_DURATION_MS = 50;

    @Unique
    private static boolean isAprilFools() {
        LocalDate today = LocalDate.now();
        return today.getMonthValue() == SzarClient.april && today.getDayOfMonth() == SzarClient.fools;
    }

    @Unique
    private static void loadFrames() {
        if (framesLoaded) return;
        framesLoaded = true;

        MinecraftClient client = MinecraftClient.getInstance();
        ResourceManager resourceManager = client.getResourceManager();

        try (InputStream stream = resourceManager.getResource(SOURCE_TEXTURE).get().getInputStream()) {
            NativeImage full = NativeImage.read(stream);

            int frameSize = full.getWidth(); // 720
            int totalFrames = full.getHeight() / frameSize; // 40

            for (int i = 0; i < totalFrames; i++) {
                NativeImage frame = new NativeImage(frameSize, frameSize, false);
                int yOffset = i * frameSize;

                for (int px = 0; px < frameSize; px++) {
                    for (int py = 0; py < frameSize; py++) {
                        frame.setColor(px, py, full.getColor(px, yOffset + py));
                    }
                }

                Identifier frameId = new Identifier("szar", "aprilfools/overlay_frame_" + i);
                NativeImageBackedTexture texture = new NativeImageBackedTexture(frame);
                client.getTextureManager().registerTexture(frameId, texture);
                FRAMES.add(frameId);
            }

            full.close();
        } catch (Exception e) {
            System.err.println("[Szar] Failed to load april fools overlay frames: " + e);
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!isAprilFools()) return;
        loadFrames();

        long now = System.currentTimeMillis();
        if (now - lastFrameTime >= FRAME_DURATION_MS) {
            currentFrame = (currentFrame + 1) % FRAMES.size();
            lastFrameTime = now;
        }
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lnet/minecraft/util/Identifier;IIIIFFIIII)V"
            )
    )
    private void redirectPanoramaOverlay(DrawContext context, Identifier texture,
                                         int x, int y, int width, int height,
                                         float u, float v,
                                         int regionWidth, int regionHeight,
                                         int textureWidth, int textureHeight) {
        if (isAprilFools() && VANILLA_OVERLAY.equals(texture) && !FRAMES.isEmpty()) {
            // Each frame is a square texture so region = full texture size
            context.drawTexture(FRAMES.get(currentFrame), x, y, width, height, 0.0F, 0.0F, 720, 720, 720, 720);
        } else {
            context.drawTexture(texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
        }
    }
}