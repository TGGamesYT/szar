package dev.tggamesyt.szar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.tggamesyt.szar.NiggerEntity;
import dev.tggamesyt.szar.Szar;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.object.builder.v1.client.model.FabricModelPredicateProviderRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class SzarClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
                Szar.NWORDPACKET,
                (client, handler, buf, responseSender) -> {

                    ItemStack stack = buf.readItemStack();

                    client.execute(() -> {
                        MinecraftClient.getInstance()
                                .gameRenderer.showFloatingItem(stack);
                    });
                }
        );
        EntityRendererRegistry.register(
                Szar.NiggerEntityType,
                NiggerEntityRenderer::new
        );

        EntityRendererRegistry.register(
                Szar.GYPSY_ENTITY_TYPE,
                GypsyEntityRenderer::new
        );
        BlockRenderLayerMap.INSTANCE.putBlock(
                Szar.CANNABIS_BLOCK,
                RenderLayer.getCutout()
        );
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.player == null) return;
            if (!client.player.hasStatusEffect(Szar.DROG_EFFECT)) return;

            var effect = client.player.getStatusEffect(Szar.DROG_EFFECT);
            int amplifier = effect.getAmplifier(); // 0 = level I

            float level = amplifier + 1f;
            float time = client.player.age + tickDelta;

            /* ───── Color speed (gentle ramp) ───── */
            float speed = 0.015f + amplifier * 0.012f;
            float hue = (time * speed) % 1.0f;

            int rgb = MathHelper.hsvToRgb(hue, 0.95f, 1f);

            /* ───── Alpha (mostly stable) ───── */
            float pulse =
                    (MathHelper.sin(time * (0.04f + amplifier * 0.015f)) + 1f) * 0.5f;

            float alpha = MathHelper.clamp(
                    0.20f + amplifier * 0.10f + pulse * 0.10f,
                    0.20f,
                    0.70f
            );

            /* ───── Very subtle jitter ───── */
            float jitter = 0.15f * amplifier;
            float jitterX = (client.world.random.nextFloat() - 0.5f) * jitter;
            float jitterY = (client.world.random.nextFloat() - 0.5f) * jitter;

            int width = client.getWindow().getScaledWidth();
            int height = client.getWindow().getScaledHeight();

            int color =
                    ((int)(alpha * 255) << 24)
                            | (rgb & 0x00FFFFFF);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            drawContext.getMatrices().push();
            drawContext.getMatrices().translate(jitterX, jitterY, 0);

            drawContext.fill(0, 0, width, height, color);

            drawContext.getMatrices().pop();

            RenderSystem.disableBlend();
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;

            float scale = SmokeZoomHandler.getSmokeScale();
            if (scale > 0.51f) { // only when smoking
                client.inGameHud.spyglassScale = scale;
            }
        });

        SmokeZoomHandler.register();
        // In your mod initialization code
        FabricModelPredicateProviderRegistry.register(Szar.WEED_JOINT_ITEM, new Identifier("held"),
                (stack, world, entity, seed) -> {
                    return entity != null && entity.getMainHandStack() == stack ? 1.0f : 0.0f;
                });

    }
}
