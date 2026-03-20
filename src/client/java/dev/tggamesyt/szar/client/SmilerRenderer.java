package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.SmilerEntity;
import dev.tggamesyt.szar.SmilerType;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public class SmilerRenderer extends EntityRenderer<SmilerEntity> {

    private static final Identifier EYES_TEX =
            new Identifier("szar", "textures/entity/eyes.png");
    private static final Identifier SCARY_TEX =
            new Identifier("szar", "textures/entity/scary.png");
    private static final Identifier REAL_TEX =
            new Identifier("szar", "textures/entity/real.png");

    public SmilerRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public Identifier getTexture(SmilerEntity entity) {
        return switch (entity.smilerType) {
            case EYES -> EYES_TEX;
            case SCARY -> SCARY_TEX;
            case REAL -> REAL_TEX;
        };
    }

    @Override
    public void render(SmilerEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                       int light) {
        matrices.push();

        // Billboard — face camera
        matrices.multiply(this.dispatcher.getRotation());
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f));

        // Scale to roughly 1.5 blocks tall
        matrices.scale(1.5f, 1.5f, 1.5f);

        Matrix4f matrix = matrices.peek().getPositionMatrix();

        Identifier texture = getTexture(entity);
        VertexConsumer consumer = vertexConsumers.getBuffer(
                RenderLayer.getEntityTranslucent(texture));

        float s = 0.5f;

// Top-left quarter UVs
        float u0 = 0.0f;
        float v0 = 0.0f;
        float u1 = 0.5f;
        float v1 = 0.5f;

        int fullBright = 0xF000F0;

        consumer.vertex(matrix, -s, -s, 0).color(255, 255, 255, 255)
                .texture(u0, v1).overlay(OverlayTexture.DEFAULT_UV).light(fullBright)
                .normal(0, 1, 0).next();

        consumer.vertex(matrix, s, -s, 0).color(255, 255, 255, 255)
                .texture(u1, v1).overlay(OverlayTexture.DEFAULT_UV).light(fullBright)
                .normal(0, 1, 0).next();

        consumer.vertex(matrix, s, s, 0).color(255, 255, 255, 255)
                .texture(u1, v0).overlay(OverlayTexture.DEFAULT_UV).light(fullBright)
                .normal(0, 1, 0).next();

        consumer.vertex(matrix, -s, s, 0).color(255, 255, 255, 255)
                .texture(u0, v0).overlay(OverlayTexture.DEFAULT_UV).light(fullBright)
                .normal(0, 1, 0).next();

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}