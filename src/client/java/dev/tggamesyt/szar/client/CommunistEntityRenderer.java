package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.CommunistEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.animation.AnimationHelper;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;

public class CommunistEntityRenderer
        extends MobEntityRenderer<CommunistEntity, PlayerEntityModel<CommunistEntity>> {

    public CommunistEntityRenderer(EntityRendererFactory.Context context) {

        super(
                context,
                new PlayerEntityModel<>(context.getPart(EntityModelLayers.PLAYER), false),
                0.5F
        );

        this.addFeature(new HeldItemFeatureRenderer<>(
                this,
                context.getHeldItemRenderer()
        ));
    }

    @Override
    public Identifier getTexture(CommunistEntity entity) {
        return new Identifier("szar", "textures/entity/communist.png");
    }
}