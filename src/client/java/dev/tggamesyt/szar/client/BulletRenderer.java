package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.BulletEntity;
import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public class BulletRenderer extends EntityRenderer<BulletEntity> {

    private static final Identifier TEXTURE =
            new Identifier("szar", "textures/entity/bullet.png");

    private static final float LENGTH = 8.0F;
    private static final float WIDTH  = 2.0F;
    private static final float SCALE  = 0.0125F;

    public BulletRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(
            BulletEntity entity,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertices,
            int light
    ) {
        // Only render after 2 ticks or if partial tick is past 0.25
        if (entity.age < 2 && tickDelta <= 0.25F) return;

        net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
        if (client.player != null) {
            double dist = client.player.squaredDistanceTo(entity.getX(), entity.getY(), entity.getZ());
            if (dist < 0.25) return;
        }

        matrices.push();

        // Orient bullet to face its travel direction
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(entity.getYaw() - 90.0F));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(entity.getPitch()));

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(45.0F));
        matrices.scale(SCALE, SCALE, SCALE);

        VertexConsumer consumer = vertices.getBuffer(
                RenderLayer.getEntityCutoutNoCull(TEXTURE)
        );

        // Draw 4 crossed quads like the original
        for (int i = 0; i < 4; i++) {
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90.0F));

            Matrix4f mat = matrices.peek().getPositionMatrix();

            consumer.vertex(mat, -LENGTH, -WIDTH, 0).color(255,255,255,255).texture(0.0F, 0.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0,1,0).next();
            consumer.vertex(mat,  LENGTH, -WIDTH, 0).color(255,255,255,255).texture(1.0F, 0.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0,1,0).next();
            consumer.vertex(mat,  LENGTH,  WIDTH, 0).color(255,255,255,255).texture(1.0F, 1.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0,1,0).next();
            consumer.vertex(mat, -LENGTH,  WIDTH, 0).color(255,255,255,255).texture(0.0F, 1.0F).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0,1,0).next();
        }

        matrices.pop();

        super.render(entity, yaw, tickDelta, matrices, vertices, light);
    }

    @Override
    public Identifier getTexture(BulletEntity entity) {
        return TEXTURE;
    }
}