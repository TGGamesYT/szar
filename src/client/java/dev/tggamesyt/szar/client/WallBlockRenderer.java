package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.Szar;
import dev.tggamesyt.szar.WallBlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.render.WorldRenderer;
import org.joml.Matrix4f;

public class WallBlockRenderer implements BlockEntityRenderer<WallBlockEntity> {
    static String base = "textures/walltext/";
    static String end = ".png";
    private static final Identifier[] TEXTURES = {
            new Identifier(Szar.MOD_ID, base + "arrow1" + end),
            new Identifier(Szar.MOD_ID, base + "arrow2" + end),
            new Identifier(Szar.MOD_ID, base + "arrow3" + end),
            new Identifier(Szar.MOD_ID, base + "arrow4" + end),
            new Identifier(Szar.MOD_ID, base + "door_drawing" + end),
            new Identifier(Szar.MOD_ID, base + "wall_small_1" + end),
            new Identifier(Szar.MOD_ID, base + "wall_small_2" + end),
            new Identifier(Szar.MOD_ID, base + "window_drawing" + end),
    };

    public WallBlockRenderer(BlockEntityRendererFactory.Context ctx) {}

    @Override
    public void render(WallBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {

        if (entity.drawingIndex < 0) return; // no drawing on this block

        BlockPos pos = entity.getPos();
        int lightLevel = WorldRenderer.getLightmapCoordinates(entity.getWorld(), pos);

        Identifier texture = TEXTURES[entity.drawingIndex];
        VertexConsumer consumer = vertexConsumers.getBuffer(
                RenderLayer.getEntityCutoutNoCull(texture));

        matrices.push();

        // Offset and rotate based on which face the drawing is on
        // 0=north, 1=south, 2=west, 3=east
        switch (entity.drawingFace) {
            //case 0 -> matrices.translate(0.5, 0.5, -0.001);
            case 1 -> { // South face (positive Z)
                matrices.translate(0.5, 0.5, 1.001);
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(180));
            }
            case 2 -> { // West face (negative X)
                matrices.translate(-0.001, 0.5, 0.5);
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(90));
            }
            case 3 -> { // East face (positive X)
                matrices.translate(1.001, 0.5, 0.5);
                matrices.multiply(net.minecraft.util.math.RotationAxis.POSITIVE_Y.rotationDegrees(-90));
            }
            default -> matrices.translate(0.5, 0.5, -0.001);
        }

        // Scale to fit on the face (0.8 leaves a small border)
        matrices.scale(0.8f, 0.8f, 0.8f);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        // Draw a simple quad
        float u0 = 0f, u1 = 1f, v0 = 0f, v1 = 1f;
        float s = 0.5f; // half size

        consumer.vertex(matrix, -s, -s, 0).color(255, 255, 255, 255)
                .texture(u0, v1).overlay(overlay).light(lightLevel)
                .normal(0, 0, -1).next();
        consumer.vertex(matrix, -s, s, 0).color(255, 255, 255, 255)
                .texture(u0, v0).overlay(overlay).light(lightLevel)
                .normal(0, 0, -1).next();
        consumer.vertex(matrix, s, s, 0).color(255, 255, 255, 255)
                .texture(u1, v0).overlay(overlay).light(lightLevel)
                .normal(0, 0, -1).next();
        consumer.vertex(matrix, s, -s, 0).color(255, 255, 255, 255)
                .texture(u1, v1).overlay(overlay).light(lightLevel)
                .normal(0, 0, -1).next();

        matrices.pop();
    }
}