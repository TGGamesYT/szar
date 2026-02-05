package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.HitterEntity;
import dev.tggamesyt.szar.NiggerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

public class HitterEntityRenderer
        extends MobEntityRenderer<HitterEntity, BipedEntityModel<HitterEntity>> {

    public HitterEntityRenderer(EntityRendererFactory.Context context) {
        super(
                context,
                new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER)),
                0.5F
        );
    }

    @Override
    public Identifier getTexture(HitterEntity entity) {
        return new Identifier("szar", "textures/entity/hitter.png");
    }
}

