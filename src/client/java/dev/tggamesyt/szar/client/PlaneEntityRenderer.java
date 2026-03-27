package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.PlaneEntity;

import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class PlaneEntityRenderer extends EntityRenderer<PlaneEntity> {

    public static final EntityModelLayer MODEL_LAYER =
            new EntityModelLayer(new Identifier("szar", "plane"), "main");

    private final PlaneEntityModel model;

    public PlaneEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.model = new PlaneEntityModel(ctx.getPart(MODEL_LAYER));
    }

    @Override
    public Identifier getTexture(PlaneEntity entity) {
        return new Identifier("szar", "textures/entity/plane.png");
    }

    @Override
    public void render(
            PlaneEntity entity,
            float yaw,
            float tickDelta,
            MatrixStack matrices,
            VertexConsumerProvider vertices,
            int light
    ) {
        matrices.push();

        float interpolatedYaw   = entity.prevYaw   + (entity.getYaw()   - entity.prevYaw)   * tickDelta;
        float interpolatedPitch = entity.prevPitch + (entity.getPitch() - entity.prevPitch) * tickDelta;

        float pitchRad = (float) Math.toRadians(interpolatedPitch);
        float yawRad   = (float) Math.toRadians(interpolatedYaw);

        final float PIVOT_OFFSET = 4F;
        final float ARC_RADIUS   = 8F;

        float dy =  PIVOT_OFFSET * (1.0F - (float) Math.cos(pitchRad))
                - (ARC_RADIUS  * (1.0F - (float) Math.cos(pitchRad)) - ARC_RADIUS * (1.0F - (float) Math.cos(pitchRad)));
        float dz = ARC_RADIUS * (float) Math.sin(pitchRad);

        float worldX = -dz * (float) Math.sin(yawRad);
        float worldZ =  dz * (float) Math.cos(yawRad);
        float worldY =  PIVOT_OFFSET * (1.0F - (float) Math.cos(pitchRad));

        matrices.translate(worldX, worldY, worldZ);

        matrices.scale(4.0F, 4.0F, 4.0F);
        matrices.translate(0.0, 1.5, 0.0);

        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-interpolatedYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(interpolatedPitch));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));
        matrices.scale(-1.0F, -1.0F, 1.0F);

        model.setAngles(
                entity, 0, 0,
                entity.age + tickDelta,
                interpolatedYaw,
                interpolatedPitch
        );

        VertexConsumer consumer =
                vertices.getBuffer(RenderLayer.getEntityCutout(getTexture(entity)));

        model.render(
                matrices, consumer,
                light, OverlayTexture.DEFAULT_UV,
                1.0F, 1.0F, 1.0F, 1.0F
        );

        matrices.pop();
    }
}
