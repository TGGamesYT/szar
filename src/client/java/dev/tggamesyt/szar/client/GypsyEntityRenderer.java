package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.GypsyEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

public class GypsyEntityRenderer
        extends MobEntityRenderer<GypsyEntity, BipedEntityModel<GypsyEntity>> {

    public GypsyEntityRenderer(EntityRendererFactory.Context context) {
        super(
                context,
                new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER)),
                0.5F
        );
        // 🔥 THIS is what makes items appear in hands
        this.addFeature(new HeldItemFeatureRenderer<>(
                this,
                context.getHeldItemRenderer()
        ));
    }

    @Override
    public Identifier getTexture(GypsyEntity entity) {
        return new Identifier("szar", "textures/entity/gypsy.png");
    }
}

