package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.NaziEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.HeldItemFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Identifier;

public class NaziEntityRenderer
        extends MobEntityRenderer<NaziEntity, PlayerEntityModel<NaziEntity>> {

    public NaziEntityRenderer(EntityRendererFactory.Context context) {
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
    public Identifier getTexture(NaziEntity entity) {
        return new Identifier("szar", "textures/entity/nazi.png");
    }
}

