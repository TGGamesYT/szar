package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.BackroomsLightBlockEntity;
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

    // Your light texture
    private static final Identifier TEXTURE =
            new Identifier("szar", "textures/block/white.png");

    public BackroomsLightBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(BackroomsLightBlockEntity entity, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                       int light, int overlay) {

        float brightness = entity.brightness;
        if (brightness <= 0.0f) return; // fully dark, don't render

        BlockPos pos = entity.getPos();
        int lightLevel = WorldRenderer.getLightmapCoordinates(entity.getWorld(), pos);

        VertexConsumer consumer = vertexConsumers.getBuffer(
                RenderLayer.getEntityCutoutNoCull(TEXTURE));

        matrices.push();
        // Center on block, render on bottom face (light faces downward into room)
        matrices.translate(0.5, 0.001, 0.5);
        matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_X.rotationDegrees(90));

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Apply brightness as color multiplier
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
}