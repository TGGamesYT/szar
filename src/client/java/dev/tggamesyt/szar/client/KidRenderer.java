package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.KidEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class KidRenderer extends MobEntityRenderer<KidEntity, PlayerEntityModel<KidEntity>> {

    private static final float MAX_HEAD_SCALE = 2.0f;
    private static final float MIN_BODY_SCALE = 0.3f; // starting scale for body
    private static final float MAX_BODY_SCALE = 1.0f; // final player scale

    public KidRenderer(EntityRendererFactory.Context ctx) {
        super(ctx,
                new PlayerEntityModel<>(ctx.getPart(EntityModelLayers.PLAYER), false),
                0.5f);
    }

    @Override
    public Identifier getTexture(KidEntity entity) {
        return HybridSkinManager.getHybridSkin(entity);
    }

    @Override
    public void scale(KidEntity entity, MatrixStack matrices, float tickDelta) {
        // Calculate growth fraction
        float ageFraction = entity.getAgeFraction(); // we’ll add this helper in KidEntity
        if (ageFraction > 1f) ageFraction = 1f;

        // Scale body gradually from MIN_BODY_SCALE → MAX_BODY_SCALE
        float bodyScale = MIN_BODY_SCALE + (MAX_BODY_SCALE - MIN_BODY_SCALE) * ageFraction;
        matrices.scale(bodyScale, bodyScale, bodyScale);

        // Scale head separately (start huge, shrink to normal)
        PlayerEntityModel<?> model = this.getModel();
        float headScale = MAX_HEAD_SCALE - (MAX_HEAD_SCALE - 1.0f) * ageFraction;
        model.head.xScale = headScale;
        model.head.yScale = headScale;
        model.head.zScale = headScale;
        // sleeping pose
        if (entity.isSleeping()) {
            matrices.translate(0, -0.5, 0); // lower the body
            model.body.pitch = 90f;        // lay on side
        }
        super.scale(entity, matrices, tickDelta);
    }
}