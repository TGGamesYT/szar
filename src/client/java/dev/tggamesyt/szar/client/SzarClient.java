package dev.tggamesyt.szar.client;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.tggamesyt.szar.NiggerEntity;
import dev.tggamesyt.szar.Szar;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
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
        BlockRenderLayerMap.INSTANCE.putBlock(
                Szar.CANNABIS_BLOCK,
                RenderLayer.getCutout()
        );
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.player != null && client.player.hasStatusEffect(Szar.DROG_EFFECT)) {
                float time = client.player.age + client.getTickDelta();
                float hue = (time * 0.01f) % 1f;
                int rgb = MathHelper.hsvToRgb(hue, 1f, 1f);

                float alpha = 0.25f;
                float r = ((rgb >> 16) & 0xFF) / 255f;
                float g = ((rgb >> 8) & 0xFF) / 255f;
                float b = (rgb & 0xFF) / 255f;

                int width = client.getWindow().getFramebufferWidth();
                int height = client.getWindow().getFramebufferHeight();

                RenderSystem.disableDepthTest();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();

                // ✅ Correct shader for 1.20.1 colored quads
                RenderSystem.setShader(GameRenderer::getPositionColorProgram);

                BufferBuilder buffer = Tessellator.getInstance().getBuffer();
                buffer.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

                buffer.vertex(0, height, 0).color(r, g, b, alpha).next();
                buffer.vertex(width, height, 0).color(r, g, b, alpha).next();
                buffer.vertex(width, 0, 0).color(r, g, b, alpha).next();
                buffer.vertex(0, 0, 0).color(r, g, b, alpha).next();

                Tessellator.getInstance().draw();

                RenderSystem.disableBlend();
                RenderSystem.enableDepthTest();
            }
        });


    }
}
