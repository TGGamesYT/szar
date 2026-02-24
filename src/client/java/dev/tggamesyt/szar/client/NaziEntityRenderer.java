package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.NaziEntity;
import dev.tggamesyt.szar.client.mixin.PlayerModelMixin;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.animation.AnimationHelper;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;

import java.util.Random;

public class NaziEntityRenderer
        extends MobEntityRenderer<NaziEntity, PlayerEntityModel<NaziEntity>> {

    private final Vector3f tempVec = new Vector3f();
    final PlayerModelAdapter<NaziEntity> adapter;

    public NaziEntityRenderer(EntityRendererFactory.Context context) {

        super(
                context,
                new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false),
                0.5F
        );

        PlayerEntityModel<NaziEntity> base = (PlayerEntityModel<NaziEntity>) this.getModel();

        PlayerModelAdapter<NaziEntity> a = new PlayerModelAdapter<>(base);
        a.setRoot(context.getPart(EntityModelLayers.PLAYER));
        this.adapter = a;

        this.addFeature(new HeldItemFeatureRenderer<>(
                this,
                context.getHeldItemRenderer()
        ));
    }

    @Override
    public Identifier getTexture(NaziEntity entity) {
        return new Identifier("szar", "textures/entity/nazi.png");
    }

    @Override
    public void render(NaziEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {

        // ehh this shit doesnt work

        final Vector3f tempVec = new Vector3f();
        // Check if the entity is currently playing the hand animation
        if (entity.isPlayingHandAnim()) {
            // Apply the hand animation directly from PlayerAnimations
            long elapsedMillis = (long)(entity.getAnimationProgress() * 1000f);
            AnimationHelper.animate(
                    adapter,
                    PlayerAnimations.hithand,
                    elapsedMillis,
                    1.0F,
                    tempVec
            );
        }

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }
}