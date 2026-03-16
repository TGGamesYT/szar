package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.StalinEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

public class StalinEntityRenderer
        extends MobEntityRenderer<StalinEntity, BipedEntityModel<StalinEntity>> {

    public StalinEntityRenderer(EntityRendererFactory.Context context) {
        super(
                context,
                new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER)),
                0.5F
        );
    }

    @Override
    public Identifier getTexture(StalinEntity entity) {
        return new Identifier("szar", "textures/entity/stalin.png");
    }
}

