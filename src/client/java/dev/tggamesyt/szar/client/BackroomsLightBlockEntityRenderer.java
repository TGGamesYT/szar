package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.BackroomsLightBlock;
import dev.tggamesyt.szar.BackroomsLightBlockEntity;
import dev.tggamesyt.szar.BackroomsLightManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.joml.Matrix4f;

public class BackroomsLightBlockEntityRenderer implements BlockEntityRenderer<BackroomsLightBlockEntity> {

    private static final Identifier TEXTURE =
            new Identifier("szar", "textures/block/white.png");

    public BackroomsLightBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(BackroomsLightBlockEntity entity, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                       int light, int overlay) {

        BlockState state = entity.getCachedState();
        BackroomsLightBlock.LightState ls = state.get(BackroomsLightBlock.LIGHT_STATE);

        float brightness;
        if (ls == BackroomsLightBlock.LightState.OFF) {
            brightness = 0.0f;
        } else if (ls == BackroomsLightBlock.LightState.ON) {
            // Check if there's a global flicker event — if so flicker this light too
            if (BackroomsLightManager.currentEvent == BackroomsLightManager.GlobalEvent.FLICKER) {
                brightness = computeFlicker(entity, tickDelta);
            } else {
                brightness = 1.0f;
            }
        } else {
            // FLICKERING — always flicker regardless of event
            brightness = computeFlicker(entity, tickDelta);
        }

        BlockPos pos = entity.getPos();
        int lightLevel = brightness > 0
                ? WorldRenderer.getLightmapCoordinates(entity.getWorld(), pos)
                : 0;

        VertexConsumer consumer = vertexConsumers.getBuffer(
                RenderLayer.getEntityCutoutNoCull(TEXTURE));

        matrices.push();
        matrices.translate(0.5, -0.001, 0.5);
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(90));

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        int r = (int)(255 * brightness);
        int g = (int)(255 * brightness);
        int b = (int)(255 * brightness);

        float s = 0.5f;
        consumer.vertex(matrix, -s, -s, 0).color(r, g, b, 255)
                .texture(0, 1).overlay(overlay).light(lightLevel)
                .normal(0, -1, 0).next();
        consumer.vertex(matrix, s, -s, 0).color(r, g, b, 255)
                .texture(1, 1).overlay(overlay).light(lightLevel)
                .normal(0, -1, 0).next();
        consumer.vertex(matrix, s, s, 0).color(r, g, b, 255)
                .texture(1, 0).overlay(overlay).light(lightLevel)
                .normal(0, -1, 0).next();
        consumer.vertex(matrix, -s, s, 0).color(r, g, b, 255)
                .texture(0, 0).overlay(overlay).light(lightLevel)
                .normal(0, -1, 0).next();

        matrices.pop();
    }

    private float computeFlicker(BackroomsLightBlockEntity entity, float tickDelta) {
        // Sample a pseudo-random noise function using global timer + per-block offset
        // This produces independent flicker per block with no server communication
        int t = BackroomsLightManager.globalFlickerTimer + entity.flickerOffset;

        // Multi-frequency noise for natural-looking flicker
        double n = Math.sin(t * 0.3) * 0.3
                + Math.sin(t * 0.7 + entity.flickerOffset) * 0.2
                + Math.sin(t * 1.3 + entity.flickerOffset * 0.5) * 0.1;

        // Occasionally snap to near-zero for a sharp flicker
        boolean snap = ((t + entity.flickerOffset) % 23) < 2;
        if (snap) return 0.05f + (float)(Math.random() * 0.1);

        float base = 0.75f + (float)(n * 0.8);
        return Math.max(0.05f, Math.min(1.0f, base));
    }
}