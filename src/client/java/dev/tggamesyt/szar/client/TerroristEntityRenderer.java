package dev.tggamesyt.szar.client;

import com.google.common.collect.ImmutableSortedMap;
import dev.tggamesyt.szar.GypsyEntity;
import dev.tggamesyt.szar.IslamTerrorist;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.util.Identifier;

public class TerroristEntityRenderer
        extends MobEntityRenderer<IslamTerrorist, BipedEntityModel<IslamTerrorist>> {

    public TerroristEntityRenderer(EntityRendererFactory.Context context) {
        super(
                context,
                new BipedEntityModel<>(context.getPart(EntityModelLayers.PLAYER)),
                0.5F
        );
    }

    @Override
    public Identifier getTexture(IslamTerrorist entity) {
        return new Identifier("szar", "textures/entity/islam_terrorist.png");
    }
}

