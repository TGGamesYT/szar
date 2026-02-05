package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.PlaneEntity;

import net.minecraft.client.render.*;
import net.minecraft.client.render.entity.*;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

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
        matrices.scale(4.0F, 4.0F, 4.0F);
        matrices.translate(0.0, 1.5, 0.0);
        matrices.scale(-1.0F, -1.0F, 1.0F);

        model.setAngles(
                entity,
                0,
                0,
                entity.age + tickDelta,
                0,
                0
        );

        VertexConsumer consumer =
                vertices.getBuffer(RenderLayer.getEntityCutout(getTexture(entity)));

        model.render(matrices, consumer, light, OverlayTexture.DEFAULT_UV,
                1.0F, 1.0F, 1.0F, 1.0F);

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertices, light);
    }
}
