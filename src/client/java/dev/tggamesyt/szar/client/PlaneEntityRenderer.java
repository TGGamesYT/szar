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

        // Smooth interpolation of rotation
        float interpolatedYaw = entity.prevYaw + (entity.getYaw() - entity.prevYaw) * tickDelta;
        float interpolatedPitch = entity.prevPitch + (entity.getPitch() - entity.prevPitch) * tickDelta;

        // Scale
        matrices.scale(4.0F, 4.0F, 4.0F);

        // Move model to correct pivot point
        matrices.translate(0.0, 1.5, 0.0);

        // Rotate to match hitbox exactly
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-interpolatedYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(interpolatedPitch));

        // Rotate 180° to fix backwards-facing
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0F));

        // Flip model upright (Minecraft model fix)
        matrices.scale(-1.0F, -1.0F, 1.0F);

        // Set model angles
        model.setAngles(
                entity,
                0,
                0,
                entity.age + tickDelta,
                interpolatedYaw,
                interpolatedPitch
        );

        VertexConsumer consumer =
                vertices.getBuffer(RenderLayer.getEntityCutout(getTexture(entity)));

        model.render(
                matrices,
                consumer,
                light,
                OverlayTexture.DEFAULT_UV,
                1.0F, 1.0F, 1.0F, 1.0F
        );

        matrices.pop();
    }
}
