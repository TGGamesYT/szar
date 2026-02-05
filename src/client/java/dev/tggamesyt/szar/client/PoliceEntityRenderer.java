package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.NiggerEntity;
import dev.tggamesyt.szar.PoliceEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

public class PoliceEntityRenderer
        extends MobEntityRenderer<PoliceEntity, BipedEntityModel<PoliceEntity>> {

    public PoliceEntityRenderer(EntityRendererFactory.Context context) {
        super(
                context,
                new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER)),
                0.5F
        );
    }

    @Override
    public Identifier getTexture(PoliceEntity entity) {
        return new Identifier("szar", "textures/entity/police-man.png");
    }
}

