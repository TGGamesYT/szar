package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.NaziEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

public class NaziEntityRenderer
        extends MobEntityRenderer<NaziEntity, BipedEntityModel<NaziEntity>> {

    public NaziEntityRenderer(EntityRendererFactory.Context context) {
        super(
                context,
                new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER)),
                0.5F
        );
    }

    @Override
    public Identifier getTexture(NaziEntity entity) {
        return new Identifier("szar", "textures/entity/nazi.png");
    }
}

