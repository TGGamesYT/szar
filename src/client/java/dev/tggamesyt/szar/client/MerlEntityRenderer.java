package dev.tggamesyt.szar.client;

import dev.tggamesyt.szar.MerlEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

public class MerlEntityRenderer
        extends MobEntityRenderer<MerlEntity, BipedEntityModel<MerlEntity>> {

    public MerlEntityRenderer(EntityRendererFactory.Context context) {
        super(
                context,
                new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER_SLIM)),
                0.5F
        );
    }

    @Override
    public Identifier getTexture(MerlEntity entity) {
        return new Identifier("szar", "textures/entity/merl.png");
    }
}

