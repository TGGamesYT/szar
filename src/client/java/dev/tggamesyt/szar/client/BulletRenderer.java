package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.BulletEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class BulletRenderer extends EntityRenderer<BulletEntity> {

    private static final Identifier TEXTURE =
            new Identifier("szar", "textures/entity/bullet.png");

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
        matrices.push();
        matrices.scale(0.25F, 0.25F, 0.25F);
        matrices.multiply(this.dispatcher.getRotation());
        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, vertices, light);
    }

    @Override
    public Identifier getTexture(BulletEntity entity) {
        return TEXTURE;
    }
}
